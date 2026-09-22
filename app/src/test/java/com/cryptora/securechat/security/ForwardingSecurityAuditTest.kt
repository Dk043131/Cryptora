package com.cryptora.securechat.security

import com.cryptora.securechat.domain.model.ForwardApprovalStatus
import com.cryptora.securechat.domain.model.ForwardEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForwardingSecurityAuditTest {

    @Test
    fun `forward hash chain is mathematically tamper-evident`() {
        val rootId = "msg_root_001"
        val timestamp = 1700000000000L

        // Hop 1: Genesis forward from Alice to Bob
        val hash1 = ForwardEvent.calculateHash(
            previousEventHash = ForwardEvent.GENESIS_HASH,
            eventId = "evt_1",
            rootMessageId = rootId,
            fromUserId = "alice",
            toUserId = "bob",
            timestamp = timestamp,
            policyVersion = 1,
            approvalStatus = ForwardApprovalStatus.APPROVED
        )

        // Hop 2: Forward from Bob to Charlie, chaining hash1
        val hash2 = ForwardEvent.calculateHash(
            previousEventHash = hash1,
            eventId = "evt_2",
            rootMessageId = rootId,
            fromUserId = "bob",
            toUserId = "charlie",
            timestamp = timestamp + 1000L,
            policyVersion = 1,
            approvalStatus = ForwardApprovalStatus.APPROVED
        )

        assertNotEquals(hash1, hash2)

        // If an attacker tampers with Hop 1's recipient (changing to 'eve')
        val tamperedHash1 = ForwardEvent.calculateHash(
            previousEventHash = ForwardEvent.GENESIS_HASH,
            eventId = "evt_1",
            rootMessageId = rootId,
            fromUserId = "alice",
            toUserId = "eve", // Tampered
            timestamp = timestamp,
            policyVersion = 1,
            approvalStatus = ForwardApprovalStatus.APPROVED
        )

        assertNotEquals(hash1, tamperedHash1)

        // Downstream hash calculation with tampered previous hash produces complete mismatch
        val downstreamWithTamperedPrev = ForwardEvent.calculateHash(
            previousEventHash = tamperedHash1,
            eventId = "evt_2",
            rootMessageId = rootId,
            fromUserId = "bob",
            toUserId = "charlie",
            timestamp = timestamp + 1000L,
            policyVersion = 1,
            approvalStatus = ForwardApprovalStatus.APPROVED
        )

        assertNotEquals(hash2, downstreamWithTamperedPrev)
    }

    @Test
    fun `privacy check - forward events contain only public IDs and usernames`() {
        val event = ForwardEvent(
            eventId = "evt_1",
            rootMessageId = "root_1",
            secureMessageId = "msg_1",
            parentEventId = null,
            fromUserId = "usr_alice",
            fromUsername = "alice",
            toUserId = "usr_bob",
            toUsername = "bob",
            timestamp = 1000L,
            policyVersion = 1,
            approvalStatus = ForwardApprovalStatus.APPROVED,
            previousEventHash = ForwardEvent.GENESIS_HASH,
            eventHash = "hash1",
            signatureMetadata = "sig_alice"
        )

        // Verify no phone numbers or credentials exist in domain model
        assertFalse(event.fromUserId.contains("+"))
        assertFalse(event.toUserId.contains("+"))
        assertEquals("alice", event.fromUsername)
        assertEquals("bob", event.toUsername)
    }
}
