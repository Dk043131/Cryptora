package com.cryptora.securechat.notes

import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.common.Resource
import com.cryptora.securechat.core.database.dao.NoteDao
import com.cryptora.securechat.core.database.entity.NoteEntity
import com.cryptora.securechat.core.security.CryptoManager
import com.cryptora.securechat.core.security.CryptoManagerImpl
import com.cryptora.securechat.core.security.KeyManager
import com.cryptora.securechat.core.security.KeyManagerImpl
import com.cryptora.securechat.data.notes.NoteRepositoryImpl
import com.cryptora.securechat.domain.model.Attachment
import com.cryptora.securechat.domain.model.EncryptionMetadata
import com.cryptora.securechat.domain.model.Message
import com.cryptora.securechat.domain.model.MessageDeliveryStatus
import com.cryptora.securechat.domain.model.MessageType
import com.cryptora.securechat.domain.model.SecureMessagePolicy
import com.cryptora.securechat.domain.repository.MessageRepository
import com.cryptora.securechat.domain.usecase.notes.CreateNote
import com.cryptora.securechat.domain.usecase.notes.DeleteNote
import com.cryptora.securechat.domain.usecase.notes.SearchNotes
import com.cryptora.securechat.domain.usecase.notes.SendNoteSecurely
import com.cryptora.securechat.domain.usecase.notes.UpdateNote
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class NotesSubsystemTest {

    private lateinit var keyManager: KeyManager
    private lateinit var cryptoManager: CryptoManager
    private lateinit var fakeNoteDao: FakeNoteDao
    private lateinit var fakeMessageRepository: FakeMessageRepository
    private lateinit var noteRepository: NoteRepositoryImpl

    private lateinit var createNote: CreateNote
    private lateinit var updateNote: UpdateNote
    private lateinit var deleteNote: DeleteNote
    private lateinit var searchNotes: SearchNotes
    private lateinit var sendNoteSecurely: SendNoteSecurely

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    }

    @Before
    fun setUp() {
        keyManager = KeyManagerImpl()
        cryptoManager = CryptoManagerImpl(keyManager)
        fakeNoteDao = FakeNoteDao()
        fakeMessageRepository = FakeMessageRepository()

        noteRepository = NoteRepositoryImpl(
            noteDao = fakeNoteDao,
            cryptoManager = cryptoManager,
            messageRepository = fakeMessageRepository,
            dispatchers = testDispatchers
        )

        createNote = CreateNote(noteRepository)
        updateNote = UpdateNote(noteRepository)
        deleteNote = DeleteNote(noteRepository)
        searchNotes = SearchNotes(noteRepository)
        sendNoteSecurely = SendNoteSecurely(noteRepository)
    }

    @Test
    fun testCreateNote_encryptsContentAtRest() = runBlocking {
        val title = "Server Credentials"
        val body = "SSH_KEY=0x99AABBCCDDEEFF"

        val createResult = createNote(title, body)
        assertTrue(createResult is Resource.Success)
        val note = (createResult as Resource.Success).data

        assertEquals(title, note.title)
        assertEquals(body, note.body)
        assertFalse(note.isPinned)

        // Verify content in the DAO is encrypted and does not contain plaintext
        val storedEntity = fakeNoteDao.getNoteById(note.id)
        assertNotNull(storedEntity)
        assertNotEquals(title, storedEntity?.encryptedTitleBase64)
        assertNotEquals(body, storedEntity?.encryptedBodyBase64)
        assertFalse(storedEntity!!.encryptedBodyBase64.contains("SSH_KEY"))
    }

    @Test
    fun testUpdateNote_updatesEncryptedPayloadAndTimestamp() = runBlocking {
        val initialResult = createNote("Meeting Notes", "Discuss Q3 roadmap")
        val note = (initialResult as Resource.Success).data

        val updatedResult = updateNote(note.id, "Meeting Notes Final", "Discuss Q3 roadmap & Budget", false)
        assertTrue(updatedResult is Resource.Success)
        val updatedNote = (updatedResult as Resource.Success).data

        assertEquals("Meeting Notes Final", updatedNote.title)
        assertEquals("Discuss Q3 roadmap & Budget", updatedNote.body)

        val fetched = noteRepository.getNoteById(note.id)
        assertTrue(fetched is Resource.Success)
        assertEquals("Meeting Notes Final", (fetched as Resource.Success).data?.title)
    }

    @Test
    fun testPinAndUnpinNotes_maintainsPinStatus() = runBlocking {
        val res1 = createNote("Note 1", "Body 1")
        val res2 = createNote("Note 2", "Body 2")
        val note1 = (res1 as Resource.Success).data
        val note2 = (res2 as Resource.Success).data

        // Pin note 2
        val pinResult = noteRepository.togglePinNote(note2.id)
        assertTrue(pinResult is Resource.Success)
        assertTrue((pinResult as Resource.Success).data.isPinned)

        val notesList = noteRepository.getNotes().first()
        // Pinned notes appear first
        assertTrue(notesList.first().isPinned)
        assertEquals(note2.id, notesList.first().id)

        // Unpin note 2
        val unpinResult = noteRepository.togglePinNote(note2.id)
        assertTrue(unpinResult is Resource.Success)
        assertFalse((unpinResult as Resource.Success).data.isPinned)
    }

    @Test
    fun testSearchNotesLocally() = runBlocking {
        createNote("API Information", "Endpoint is /api/v1/auth")
        createNote("Shopping List", "Apples, Milk, Bread")
        createNote("Cryptora Ideas", "Add biometric lock screen")

        val searchResult1 = searchNotes("API")
        assertTrue(searchResult1 is Resource.Success)
        val list1 = (searchResult1 as Resource.Success).data
        assertEquals(1, list1.size)
        assertEquals("API Information", list1.first().title)

        val searchResult2 = searchNotes("biometric")
        assertTrue(searchResult2 is Resource.Success)
        val list2 = (searchResult2 as Resource.Success).data
        assertEquals(1, list2.size)
        assertEquals("Cryptora Ideas", list2.first().title)

        val searchResultEmpty = searchNotes("NonExistentTermXYZ")
        assertTrue(searchResultEmpty is Resource.Success)
        assertTrue((searchResultEmpty as Resource.Success).data.isEmpty())
    }

    @Test
    fun testDeleteNote_shredsNoteFromStorage() = runBlocking {
        val createResult = createNote("Temporary Token", "ghp_1234567890abcdef")
        val noteId = (createResult as Resource.Success).data.id

        assertNotNull(fakeNoteDao.getNoteById(noteId))

        val delResult = deleteNote(noteId)
        assertTrue(delResult is Resource.Success)

        // Note is removed from database
        val afterDelete = fakeNoteDao.getNoteById(noteId)
        assertEquals(null, afterDelete)

        val notesList = noteRepository.getNotes().first()
        assertTrue(notesList.none { it.id == noteId })
    }

    @Test
    fun testSendNoteSecurely_convertsToEncryptedChatMessageWithoutManualCopy() = runBlocking {
        val createResult = createNote("Project Credentials", "DB_PASS=SuperSecret123!")
        val noteId = (createResult as Resource.Success).data.id

        val convId = "conv_alice_bob"
        val recipientId = "user_bob"

        val sendResult = sendNoteSecurely(noteId, convId, recipientId)
        assertTrue(sendResult is Resource.Success)
        val sentMessage = (sendResult as Resource.Success).data

        assertEquals(convId, sentMessage.conversationId)
        assertEquals(recipientId, sentMessage.recipientId)
        assertEquals(MessageType.TEXT, sentMessage.messageType)
        assertTrue(sentMessage.decryptedTextCache?.contains("Project Credentials") == true)
        assertTrue(sentMessage.decryptedTextCache?.contains("DB_PASS=SuperSecret123!") == true)

        // Verify sent message was captured in message repository
        assertEquals(1, fakeMessageRepository.sentMessages.size)
        val captured = fakeMessageRepository.sentMessages.first()
        assertEquals(convId, captured.conversationId)
    }

    // Fake in-memory DAO for unit testing
    private class FakeNoteDao : NoteDao {
        private val notesMap = mutableMapOf<String, NoteEntity>()
        private val flow = MutableStateFlow<List<NoteEntity>>(emptyList())

        private fun updateFlow() {
            flow.value = notesMap.values.sortedWith(
                compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.updatedAt }
            )
        }

        override fun getNotesFlow(): Flow<List<NoteEntity>> = flow

        override suspend fun getNoteById(id: String): NoteEntity? = notesMap[id]

        override suspend fun insertOrUpdate(note: NoteEntity) {
            notesMap[note.id] = note
            updateFlow()
        }

        override suspend fun deleteNote(id: String) {
            notesMap.remove(id)
            updateFlow()
        }

        override suspend fun updatePinStatus(id: String, isPinned: Boolean, updatedAt: Long) {
            notesMap[id]?.let {
                notesMap[id] = it.copy(isPinned = isPinned, updatedAt = updatedAt)
                updateFlow()
            }
        }
    }

    // Fake message repository to verify SendNoteSecurely interaction
    private class FakeMessageRepository : MessageRepository {
        val sentMessages = mutableListOf<Message>()

        override fun getMessages(conversationId: String): Flow<List<Message>> =
            MutableStateFlow(sentMessages.filter { it.conversationId == conversationId })

        override suspend fun getMessagesPaged(
            conversationId: String,
            limit: Int,
            offset: Int
        ): Resource<List<Message>> = Resource.Success(sentMessages)

        override suspend fun sendTextMessage(
            conversationId: String,
            recipientId: String,
            text: String
        ): Resource<Message> = sendTextMessage(conversationId, recipientId, text, SecureMessagePolicy())

        override suspend fun sendTextMessage(
            conversationId: String,
            recipientId: String,
            text: String,
            policy: SecureMessagePolicy
        ): Resource<Message> {
            val msg = Message(
                id = "msg_${System.currentTimeMillis()}",
                conversationId = conversationId,
                senderId = "me",
                recipientId = recipientId,
                encryptedContentBase64 = "encrypted_base64",
                encryptionMetadata = EncryptionMetadata(initializationVectorBase64 = "", keyAlias = "test"),
                policy = policy,
                deliveryStatus = MessageDeliveryStatus.SENT,
                messageType = MessageType.TEXT,
                decryptedTextCache = text
            )
            sentMessages.add(msg)
            return Resource.Success(msg)
        }

        override suspend fun sendAttachmentMessage(
            conversationId: String,
            recipientId: String,
            uri: android.net.Uri,
            messageType: MessageType
        ): Resource<Message> = sendAttachmentMessage(conversationId, recipientId, uri, messageType, SecureMessagePolicy())

        override suspend fun sendAttachmentMessage(
            conversationId: String,
            recipientId: String,
            uri: android.net.Uri,
            messageType: MessageType,
            policy: SecureMessagePolicy
        ): Resource<Message> = Resource.Error(com.cryptora.securechat.core.common.AppError.Unknown("Not used"))

        override suspend fun retryMessage(messageId: String): Resource<Message> =
            Resource.Error(com.cryptora.securechat.core.common.AppError.Unknown("Not used"))

        override suspend fun markMessageAsRead(messageId: String): Resource<Unit> = Resource.Success(Unit)

        override suspend fun decryptMessage(message: Message): Resource<String> =
            Resource.Success(message.decryptedTextCache ?: "")

        override suspend fun decryptAttachment(attachment: Attachment): Resource<File> =
            Resource.Error(com.cryptora.securechat.core.common.AppError.Unknown("Not used"))

        override suspend fun revokeMessage(messageId: String): Resource<Unit> = Resource.Success(Unit)

        override suspend fun purgeExpiredMessages(): Resource<Int> = Resource.Success(0)

        override suspend fun sendEncryptedMessage(message: Message): Resource<Message> = Resource.Success(message)

        override suspend fun searchLocalMessages(query: String): Resource<List<Message>> =
            Resource.Success(sentMessages.filter { it.decryptedTextCache?.contains(query, ignoreCase = true) == true })

        override suspend fun deleteLocalMessage(messageId: String): Resource<Unit> {
            sentMessages.removeAll { it.id == messageId }
            return Resource.Success(Unit)
        }
    }
}
