package com.cryptora.securechat.core.demo

import com.cryptora.securechat.core.common.Constants
import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.database.CryptoraDatabase
import com.cryptora.securechat.core.database.entity.AccessRequestEntity
import com.cryptora.securechat.core.database.entity.ConversationEntity
import com.cryptora.securechat.core.database.entity.MessageEntity
import com.cryptora.securechat.core.database.entity.NoteEntity
import com.cryptora.securechat.core.security.CryptoManager
import com.cryptora.securechat.core.security.KeyManager
import com.cryptora.securechat.core.security.PasswordHasher
import com.cryptora.securechat.core.security.SecureStorage
import com.cryptora.securechat.data.auth.PersistedAccount
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds rich, realistic demonstration data across Cryptora:
 * - Verified Contacts & Active Conversations (with timelock and delivery statuses)
 * - Hardware-encrypted Notes (Backup keys, protocol checklists, endpoints)
 * - Zero-Knowledge Access Requests
 * - Demo User Accounts for instant login
 */
@Singleton
class DemoDataSeeder @Inject constructor(
    private val database: CryptoraDatabase,
    private val secureStorage: SecureStorage,
    private val passwordHasher: PasswordHasher,
    private val keyManager: KeyManager,
    private val cryptoManager: CryptoManager,
    private val dispatchers: DispatcherProvider
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun seedAllDemoData() = withContext(dispatchers.io) {
        seedAccounts()
        seedNotes()
        seedConversationsAndMessages()
        seedAccessRequests()
    }

    private suspend fun seedAccounts() {
        val demoUsers = listOf(
            Triple("deepak", "Deepak Kumar", "+919952145182"),
            Triple("dk", "DK", "+919952145180"),
            Triple("sarumathy", "Sarumathy", "+919952145181"),
            Triple("karthiga", "Karthiga", "+919952145183"),
            Triple("alex_rivera", "Alex Rivera", "+15551234567"),
            Triple("harshanth", "Harshanth", "+919876543210")
        )

        for ((username, fullName, mobile) in demoUsers) {
            val key = "user_account_$username"
            if (secureStorage.getString(key).isNullOrBlank()) {
                val saltedHash = passwordHasher.hashPassword("Password123!")
                val account = PersistedAccount(
                    id = "usr_${username}_demo",
                    username = username,
                    fullName = fullName,
                    mobileNumber = mobile,
                    passwordSaltedHash = saltedHash,
                    publicKey = "pk_cryptora_${username}_demo",
                    avatarUrl = when (username) {
                        "deepak" -> "🛡️"
                        "dk" -> "⚡"
                        "sarumathy" -> "🌸"
                        "karthiga" -> "🌟"
                        "harshanth" -> "⚡"
                        else -> "👤"
                    },
                    bio = when (username) {
                        "dk" -> "Cryptora Core Architecture • Enclave Lead"
                        "sarumathy" -> "Hardware Security Specialist • AES-GCM"
                        "karthiga" -> "Quantum Key Distribution Contributor"
                        else -> "Cryptora Secure Enclave User • E2EE Verified"
                    },
                    createdAt = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                )
                secureStorage.saveString(key, json.encodeToString(account))
            }
        }
    }

    private suspend fun seedNotes() {
        val existing = database.noteDao().getNotesFlow().firstOrNull() ?: emptyList()
        if (existing.isNotEmpty()) return

        keyManager.getOrCreateMasterKey(Constants.MASTER_KEY_ALIAS)
        val now = System.currentTimeMillis()

        val demoNotes = listOf(
            Triple(
                "🔐 Master Enclave Backup & Recovery Keys",
                "Primary Recovery Seed:\nquantum-orbit-crypto-shield-enclave-vault-2026-auth\n\nSecurity Status: Stored in AndroidKeyStore hardware enclave with AES-GCM-256 authenticated encryption.\nDO NOT EXPORT TO PLAIN TEXT.",
                true
            ),
            Triple(
                "🛡️ Zero-Knowledge Protocol Audit Checklist",
                "✓ Hardware Keystore key rotation policy active (90-day intervals)\n✓ Authoritative NTP server synchronization verified\n✓ Ephemeral ratchet state wiped on message expiry\n✓ In-memory zeroization upon session lock\n✓ Zero plaintext stored in SQLite databases",
                true
            ),
            Triple(
                "⚡ Server Infrastructure & Relay Endpoints",
                "Production Relay: https://cryptora-9zrj.onrender.com\nWebsocket Gateway: wss://cryptora-9zrj.onrender.com\nZero-knowledge policy registry active on port 10000.",
                false
            ),
            Triple(
                "📋 Emergency Enclave Lockdown Procedure",
                "To trigger immediate hardware purge:\n1. Open Enclave Settings -> Emergency Self-Destruct.\n2. Master session tokens and decrypted caches are shredded within 50ms.\n3. Hardware keys invalidated.",
                false
            )
        )

        demoNotes.forEachIndexed { index, (title, body, isPinned) ->
            val encTitle = cryptoManager.encryptString(title, Constants.MASTER_KEY_ALIAS)
            val encBody = cryptoManager.encryptString(body, Constants.MASTER_KEY_ALIAS)

            database.noteDao().insertOrUpdate(
                NoteEntity(
                    id = "note_demo_${index + 1}",
                    encryptedTitleBase64 = encTitle,
                    encryptedBodyBase64 = encBody,
                    keyAlias = Constants.MASTER_KEY_ALIAS,
                    isPinned = isPinned,
                    createdAt = now - (index * 3600000L),
                    updatedAt = now - (index * 1800000L)
                )
            )
        }
    }

    private suspend fun seedConversationsAndMessages() {
        val now = System.currentTimeMillis()
        keyManager.getOrCreateMasterKey(Constants.MASTER_KEY_ALIAS)

        // 1. Conversation with DK
        val convDkId = "conv_demo_dk"
        if (database.conversationDao().getConversationById(convDkId) == null) {
            database.conversationDao().insertOrUpdate(
                ConversationEntity(
                    id = convDkId,
                    participantUserId = "usr_dk_01",
                    participantUsername = "dk",
                    participantFullName = "DK",
                    lastMessageText = "Received encrypted bundle. Will review and verify on hardware keystore.",
                    lastMessageTimestamp = now - 5 * 60 * 1000L,
                    lastMessageDeliveryStatus = "READ",
                    unreadCount = 1,
                    isSecretSession = true,
                    updatedAt = now - 5 * 60 * 1000L
                )
            )

            val dkMessages = listOf(
                MessageSeed(
                    id = "msg_dk_1",
                    text = "Hey DK, I verified the hardware enclave encryption and real-time OTP delivery.",
                    isOutgoing = true,
                    status = "READ",
                    timestamp = now - 60 * 60000L
                ),
                MessageSeed(
                    id = "msg_dk_2",
                    text = "Awesome Deepak! The Post-Quantum key exchange and timelock channels are rock solid.",
                    isOutgoing = false,
                    status = "READ",
                    timestamp = now - 40 * 60000L
                ),
                MessageSeed(
                    id = "msg_dk_3",
                    text = "Shared the enclave master bypass credentials with a 1-hour timelock.",
                    isOutgoing = true,
                    status = "READ",
                    timestamp = now - 20 * 60000L,
                    accessMode = "TIMELOCKED_ACCESS",
                    expiresAt = now + 3600000L
                ),
                MessageSeed(
                    id = "msg_dk_4",
                    text = "Received encrypted bundle. Will review and verify on hardware keystore.",
                    isOutgoing = false,
                    status = "READ",
                    timestamp = now - 5 * 60000L
                )
            )
            insertMessages(convDkId, "usr_dk_01", dkMessages)
        }

        // 2. Conversation with Sarumathy
        val convSarumathyId = "conv_demo_sarumathy"
        if (database.conversationDao().getConversationById(convSarumathyId) == null) {
            database.conversationDao().insertOrUpdate(
                ConversationEntity(
                    id = convSarumathyId,
                    participantUserId = "usr_sarumathy_02",
                    participantUsername = "sarumathy",
                    participantFullName = "Sarumathy",
                    lastMessageText = "Yes, verified! Zero plaintext traces left in memory or SQLite database.",
                    lastMessageTimestamp = now - 12 * 60 * 1000L,
                    lastMessageDeliveryStatus = "READ",
                    unreadCount = 0,
                    isSecretSession = true,
                    updatedAt = now - 12 * 60 * 1000L
                )
            )

            val sarumathyMessages = listOf(
                MessageSeed(
                    id = "msg_s_1",
                    text = "Hi Deepak, security audit for hardware keystore passes all zero-knowledge constraints.",
                    isOutgoing = false,
                    status = "READ",
                    timestamp = now - 90 * 60000L
                ),
                MessageSeed(
                    id = "msg_s_2",
                    text = "Great work Sarumathy! Did you test forward secrecy on ephemeral ratchets?",
                    isOutgoing = true,
                    status = "READ",
                    timestamp = now - 45 * 60000L
                ),
                MessageSeed(
                    id = "msg_s_3",
                    text = "Yes, verified! Zero plaintext traces left in memory or SQLite database.",
                    isOutgoing = false,
                    status = "READ",
                    timestamp = now - 12 * 60000L
                )
            )
            insertMessages(convSarumathyId, "usr_sarumathy_02", sarumathyMessages)
        }

        // 3. Conversation with Karthiga
        val convKarthigaId = "conv_demo_karthiga"
        if (database.conversationDao().getConversationById(convKarthigaId) == null) {
            database.conversationDao().insertOrUpdate(
                ConversationEntity(
                    id = convKarthigaId,
                    participantUserId = "usr_karthiga_03",
                    participantUsername = "karthiga",
                    participantFullName = "Karthiga",
                    lastMessageText = "Everything is verified. Ready for deployment!",
                    lastMessageTimestamp = now - 25 * 60 * 1000L,
                    lastMessageDeliveryStatus = "READ",
                    unreadCount = 0,
                    isSecretSession = true,
                    updatedAt = now - 25 * 60 * 1000L
                )
            )

            val karthigaMessages = listOf(
                MessageSeed(
                    id = "msg_k_1",
                    text = "Hey Deepak, Quantum Key Distribution simulation is fully linked with the server relay.",
                    isOutgoing = false,
                    status = "READ",
                    timestamp = now - 120 * 60000L
                ),
                MessageSeed(
                    id = "msg_k_2",
                    text = "Superb Karthiga. The UI animations and secure access requests are flowing properly now.",
                    isOutgoing = true,
                    status = "READ",
                    timestamp = now - 50 * 60000L
                ),
                MessageSeed(
                    id = "msg_k_3",
                    text = "Everything is verified. Ready for deployment!",
                    isOutgoing = false,
                    status = "READ",
                    timestamp = now - 25 * 60000L
                )
            )
            insertMessages(convKarthigaId, "usr_karthiga_03", karthigaMessages)
        }

        // 4. Conversation with Harshanth
        val convHarshanthId = "conv_demo_harshanth"
        if (database.conversationDao().getConversationById(convHarshanthId) == null) {
            database.conversationDao().insertOrUpdate(
                ConversationEntity(
                    id = convHarshanthId,
                    participantUserId = "usr_harshanth_01",
                    participantUsername = "harshanth",
                    participantFullName = "Harshanth",
                    lastMessageText = "Got it! Access grant requested. Awaiting enclave authorization.",
                    lastMessageTimestamp = now - 35 * 60 * 1000L,
                    lastMessageDeliveryStatus = "READ",
                    unreadCount = 1,
                    isSecretSession = true,
                    updatedAt = now - 35 * 60 * 1000L
                )
            )

            val harshanthMessages = listOf(
                MessageSeed(
                    id = "msg_h_1",
                    text = "Hey Harshanth, did you review the updated zero-knowledge time-lock policy?",
                    isOutgoing = true,
                    status = "READ",
                    timestamp = now - 3 * 3600000L
                ),
                MessageSeed(
                    id = "msg_h_2",
                    text = "Yes! The hardware-enforced enclave looks rock solid. Encrypted payloads are decrypting within 4ms.",
                    isOutgoing = false,
                    status = "READ",
                    timestamp = now - 2 * 3600000L
                ),
                MessageSeed(
                    id = "msg_h_3",
                    text = "Awesome. I'm setting a 1-hour time-lock on the deployment credentials.",
                    isOutgoing = true,
                    status = "READ",
                    timestamp = now - 60 * 60000L,
                    accessMode = "TIMELOCKED_ACCESS",
                    expiresAt = now + 3600000L
                ),
                MessageSeed(
                    id = "msg_h_4",
                    text = "Got it! Access grant requested. Awaiting enclave authorization.",
                    isOutgoing = false,
                    status = "READ",
                    timestamp = now - 35 * 60000L
                )
            )
            insertMessages(convHarshanthId, "usr_harshanth_01", harshanthMessages)
        }

        // 5. Conversation with Elena Rostova
        val convElenaId = "conv_demo_elena"
        if (database.conversationDao().getConversationById(convElenaId) == null) {
            database.conversationDao().insertOrUpdate(
                ConversationEntity(
                    id = convElenaId,
                    participantUserId = "usr_elena_03",
                    participantUsername = "elena_k",
                    participantFullName = "Elena Rostova",
                    lastMessageText = "All enclave checks passed 100%. No memory leaks or plaintext traces detected.",
                    lastMessageTimestamp = now - 45 * 60 * 1000L,
                    lastMessageDeliveryStatus = "READ",
                    unreadCount = 0,
                    isSecretSession = true,
                    updatedAt = now - 45 * 60 * 1000L
                )
            )

            val elenaMessages = listOf(
                MessageSeed(
                    id = "msg_e_1",
                    text = "Here is the confidential security audit log for Q3.",
                    isOutgoing = false,
                    status = "READ",
                    timestamp = now - 3 * 3600000L,
                    forwardingPolicy = "FORWARDING_DISABLED"
                ),
                MessageSeed(
                    id = "msg_e_2",
                    text = "Verified the cryptographic signature against the hardware root of trust.",
                    isOutgoing = true,
                    status = "READ",
                    timestamp = now - 2 * 3600000L
                ),
                MessageSeed(
                    id = "msg_e_3",
                    text = "All enclave checks passed 100%. No memory leaks or plaintext traces detected.",
                    isOutgoing = false,
                    status = "READ",
                    timestamp = now - 45 * 60000L
                )
            )
            insertMessages(convElenaId, "usr_elena_03", elenaMessages)
        }

        // 6. Conversation with Alex Rivera
        val convAlexId = "conv_demo_alex"
        if (database.conversationDao().getConversationById(convAlexId) == null) {
            database.conversationDao().insertOrUpdate(
                ConversationEntity(
                    id = convAlexId,
                    participantUserId = "usr_alex_02",
                    participantUsername = "alex_rivera",
                    participantFullName = "Alex Rivera",
                    lastMessageText = "Approved access for 30 minutes.",
                    lastMessageTimestamp = now - 2 * 3600000L,
                    lastMessageDeliveryStatus = "READ",
                    unreadCount = 0,
                    isSecretSession = true,
                    updatedAt = now - 2 * 3600000L
                )
            )

            val alexMessages = listOf(
                MessageSeed(
                    id = "msg_a_1",
                    text = "Can you grant me temporary access to the relay policy registry?",
                    isOutgoing = false,
                    status = "READ",
                    timestamp = now - 4 * 3600000L
                ),
                MessageSeed(
                    id = "msg_a_2",
                    text = "Approved access for 30 minutes.",
                    isOutgoing = true,
                    status = "READ",
                    timestamp = now - 2 * 3600000L
                )
            )
            insertMessages(convAlexId, "usr_alex_02", alexMessages)
        }
    }

    private data class MessageSeed(
        val id: String,
        val text: String,
        val isOutgoing: Boolean,
        val status: String,
        val timestamp: Long,
        val accessMode: String = "IMMEDIATE_ACCESS",
        val expiresAt: Long? = null,
        val forwardingPolicy: String = "ALLOWED"
    )

    private suspend fun insertMessages(conversationId: String, participantId: String, seeds: List<MessageSeed>) {
        val entities = seeds.map { seed ->
            val encText = cryptoManager.encryptString(seed.text, Constants.MASTER_KEY_ALIAS)
            MessageEntity(
                id = seed.id,
                conversationId = conversationId,
                senderId = if (seed.isOutgoing) "self" else participantId,
                recipientId = if (seed.isOutgoing) participantId else "self",
                encryptedContentBase64 = encText,
                keyAlias = Constants.MASTER_KEY_ALIAS,
                ivBase64 = "",
                messageType = "TEXT",
                deliveryStatus = seed.status,
                isOutgoing = seed.isOutgoing,
                decryptedTextCache = seed.text,
                timestamp = seed.timestamp,
                hasAttachment = false,
                accessMode = seed.accessMode,
                expiresAt = seed.expiresAt,
                forwardingPolicy = seed.forwardingPolicy,
                approvalRequired = false,
                policyOwnerId = if (seed.isOutgoing) "self" else participantId,
                isAccessGranted = true,
                rootMessageId = seed.id,
                forwardCount = 0,
                isForwarded = false
            )
        }
        database.messageDao().insertAll(entities)
    }

    private suspend fun seedAccessRequests() {
        val now = System.currentTimeMillis()
        database.accessRequestDao().insertOrUpdate(
            AccessRequestEntity(
                requestId = "req_demo_01",
                messageId = "msg_h_3",
                conversationId = "conv_demo_harshanth",
                requesterId = "usr_harshanth_01",
                requesterUsername = "harshanth",
                ownerId = "self",
                contentTitle = "Production Deployment Credentials",
                requestedDuration = 30 * 60 * 1000L,
                status = "PENDING",
                requestedAt = now - 15 * 60 * 1000L
            )
        )
        database.accessRequestDao().insertOrUpdate(
            AccessRequestEntity(
                requestId = "req_demo_02",
                messageId = "msg_e_1",
                conversationId = "conv_demo_elena",
                requesterId = "usr_elena_03",
                requesterUsername = "elena_k",
                ownerId = "self",
                contentTitle = "Q3 Cryptographic Security Audit Log",
                requestedDuration = 60 * 60 * 1000L,
                status = "APPROVED",
                requestedAt = now - 2 * 3600000L,
                respondedAt = now - 110 * 60000L,
                grantedDuration = 60 * 60 * 1000L,
                expiresAt = now + 40 * 60000L
            )
        )
    }
}
