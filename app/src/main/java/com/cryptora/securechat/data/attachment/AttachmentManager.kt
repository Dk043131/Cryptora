package com.cryptora.securechat.data.attachment

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.cryptora.securechat.core.common.AppError
import com.cryptora.securechat.core.common.Constants
import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.core.security.CryptoManager
import com.cryptora.securechat.domain.model.Attachment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager,
    private val dispatchers: DispatcherProvider
) {
    companion object {
        const val MAX_ATTACHMENT_SIZE_BYTES = 25L * 1024 * 1024 // 25 MB limit
        private val DISALLOWED_EXTENSIONS = setOf("exe", "apk", "sh", "bat", "cmd", "bin", "jar", "so")
    }

    private val attachmentsDir: File
        get() = File(context.filesDir, "encrypted_attachments").apply {
            if (!exists()) mkdirs()
        }

    private val cacheDir: File
        get() = File(context.cacheDir, "decrypted_previews").apply {
            if (!exists()) mkdirs()
        }

    suspend fun processAndEncryptAttachment(
        uri: Uri,
        messageId: String,
        keyAlias: String = Constants.MASTER_KEY_ALIAS
    ): Resource<Attachment> = withContext(dispatchers.io) {
        try {
            val contentResolver = context.contentResolver

            // 1. Resolve file name and size
            var fileName = "attachment_${System.currentTimeMillis()}"
            var fileSize = -1L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            // 2. MIME type determination & validation
            var mimeType = contentResolver.getType(uri)
            if (mimeType == null || mimeType == "application/octet-stream") {
                val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
                    ?: fileName.substringAfterLast('.', "")
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
                    ?: "application/octet-stream"
            }

            // Extension validation
            val ext = fileName.substringAfterLast('.', "").lowercase()
            if (DISALLOWED_EXTENSIONS.contains(ext)) {
                return@withContext Resource.Error(
                    AppError.Validation("Executable or unsafe file types ($ext) are blocked for security.")
                )
            }

            // Size validation
            if (fileSize > MAX_ATTACHMENT_SIZE_BYTES) {
                return@withContext Resource.Error(
                    AppError.Validation("Attachment exceeds maximum allowed size of 25MB.")
                )
            }

            val attachmentId = UUID.randomUUID().toString()
            val encryptedFile = File(attachmentsDir, "${attachmentId}.enc")

            // 3. Streaming encryption from Uri inputStream directly to encryptedFile (O(1) memory)
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Resource.Error(AppError.Storage("Unable to open stream for URI: $uri"))

            val encryptedBytesWritten = inputStream.use { input ->
                FileOutputStream(encryptedFile).use { output ->
                    cryptoManager.encryptStream(input, output, keyAlias)
                }
            }

            if (fileSize <= 0) {
                fileSize = encryptedFile.length() // Approximate fallback
            }

            val attachment = Attachment(
                attachmentId = attachmentId,
                messageId = messageId,
                encryptedFileReference = encryptedFile.absolutePath,
                fileName = fileName,
                mimeType = mimeType,
                size = fileSize,
                encryptedSize = encryptedBytesWritten,
                createdAt = System.currentTimeMillis()
            )

            Resource.Success(attachment)
        } catch (e: Exception) {
            Resource.Error(AppError.Cryptography("Failed to encrypt attachment: ${e.localizedMessage}"), e)
        }
    }

    suspend fun decryptAttachmentToCache(
        attachment: Attachment,
        keyAlias: String = Constants.MASTER_KEY_ALIAS
    ): Resource<File> = withContext(dispatchers.io) {
        try {
            val encFile = File(attachment.encryptedFileReference)
            if (!encFile.exists()) {
                return@withContext Resource.Error(AppError.Storage("Encrypted attachment file not found."))
            }

            val decryptedFile = File(cacheDir, "${attachment.attachmentId}_${attachment.fileName}")
            if (decryptedFile.exists() && decryptedFile.length() > 0) {
                return@withContext Resource.Success(decryptedFile)
            }

            FileInputStream(encFile).use { input ->
                FileOutputStream(decryptedFile).use { output ->
                    cryptoManager.decryptStream(input, output, keyAlias)
                }
            }

            Resource.Success(decryptedFile)
        } catch (e: Exception) {
            Resource.Error(AppError.Cryptography("Failed to decrypt attachment: ${e.localizedMessage}"), e)
        }
    }

    fun deleteDecryptedPreview(attachment: Attachment) {
        try {
            val decryptedFile = File(cacheDir, "${attachment.attachmentId}_${attachment.fileName}")
            if (decryptedFile.exists()) {
                decryptedFile.delete()
            }
        } catch (_: Exception) {}
    }

    fun clearAllDecryptedPreviews() {
        try {
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile) file.delete()
            }
        } catch (_: Exception) {}
    }
}
