package com.cryptora.securechat.domain.usecase.chat

import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.domain.model.Attachment
import com.cryptora.securechat.domain.repository.MessageRepository
import java.io.File
import javax.inject.Inject

class DecryptAttachmentUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(attachment: Attachment): Resource<File> {
        return messageRepository.decryptAttachment(attachment)
    }
}
