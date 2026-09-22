const express = require('express');
const http = require('http');
const cors = require('cors');
const { WebSocketServer } = require('ws');

const app = express();
const server = http.createServer(app);
const wss = new WebSocketServer({ server });

const PORT = process.env.PORT || 10000;

app.use(cors());
app.use(express.json());

// In-Memory Policy & Access Store (Zero Plaintext Server Trust Model)
const serverPolicies = new Map();
const serverRequests = new Map();
const serverGrants = new Map();
const connectedClients = new Map(); // userId -> WebSocket

// --- REST Endpoints ---

app.get('/', (req, res) => {
    res.json({
        app: "Cryptora Relay Server",
        version: "1.0.0",
        status: "RUNNING",
        authoritativeServerTime: Date.now()
    });
});

app.get('/v1/health', (req, res) => {
    res.json({ status: "OK", serverTime: Date.now() });
});

app.get('/v1/time', (req, res) => {
    res.json({ serverTime: Date.now() });
});

// 1. Register Policy
app.post('/v1/policies', (req, res) => {
    const { messageId, accessMode, expiresAt, maxExpiresAt, forwardingPolicy, approvalRequired, ownerId } = req.body;
    if (!messageId || !ownerId) {
        return res.status(400).json({ error: "Missing required fields: messageId or ownerId" });
    }

    const existing = serverPolicies.get(messageId);
    if (existing && existing.ownerId !== ownerId) {
        return res.status(403).json({ error: "Unauthorized: Cannot overwrite another user's policy" });
    }

    const policy = {
        messageId,
        accessMode: accessMode || "IMMEDIATE_ACCESS",
        expiresAt: expiresAt || null,
        maxExpiresAt: maxExpiresAt || expiresAt || null,
        forwardingPolicy: forwardingPolicy || "ALLOWED",
        approvalRequired: !!approvalRequired,
        ownerId,
        authorizedUserIds: new Set([ownerId]),
        isRevoked: false,
        createdAt: Date.now(),
        policyVersion: 1
    };

    serverPolicies.set(messageId, policy);
    return res.status(201).json({ success: true, serverTime: Date.now() });
});

// 2. Validate Access
app.get('/v1/policies/:messageId/validate', (req, res) => {
    const { messageId } = req.params;
    const requesterId = req.query.requesterId;
    const serverTime = Date.now();

    const policy = serverPolicies.get(messageId);
    if (!policy) {
        return res.status(404).json({ error: "Policy not found on server" });
    }

    if (policy.isRevoked || (policy.expiresAt && serverTime >= policy.expiresAt)) {
        return res.json({
            messageId,
            isAuthorized: false,
            isExpired: true,
            accessMode: policy.accessMode,
            expiresAt: policy.expiresAt,
            authoritativeServerTime: serverTime
        });
    }

    if (requesterId === policy.ownerId || policy.accessMode === "IMMEDIATE_ACCESS") {
        return res.json({
            messageId,
            isAuthorized: true,
            isExpired: false,
            accessMode: policy.accessMode,
            expiresAt: policy.expiresAt,
            authoritativeServerTime: serverTime
        });
    }

    // Check active grant
    for (const grant of serverGrants.values()) {
        if (grant.messageId === messageId && grant.granteeId === requesterId && !grant.isRevoked) {
            const isGrantExpired = serverTime >= grant.expiresAt;
            if (isGrantExpired) {
                grant.isRevoked = true;
            }
            return res.json({
                messageId,
                isAuthorized: !isGrantExpired,
                isExpired: isGrantExpired,
                accessMode: policy.accessMode,
                expiresAt: grant.expiresAt,
                authoritativeServerTime: serverTime,
                activeGrantId: grant.grantId
            });
        }
    }

    const isAuthorized = policy.authorizedUserIds.has(requesterId);
    return res.json({
        messageId,
        isAuthorized,
        isExpired: false,
        accessMode: policy.accessMode,
        expiresAt: policy.expiresAt,
        authoritativeServerTime: serverTime
    });
});

// 3. Request Access
app.post('/v1/access-requests', (req, res) => {
    const { messageId, requesterId, requesterUsername, requestedDuration, contentTitle } = req.body;
    const serverTime = Date.now();

    const policy = serverPolicies.get(messageId);
    if (!policy) {
        return res.status(404).json({ error: "Policy not found on server" });
    }

    if (policy.isRevoked || (policy.expiresAt && serverTime >= policy.expiresAt)) {
        return res.status(410).json({ error: "Content has expired on server" });
    }

    const requestId = `req_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`;
    const requestMeta = {
        requestId,
        messageId,
        requesterId,
        requesterUsername: requesterUsername || "Anonymous",
        contentTitle: contentTitle || "Secure Content",
        requestedDuration: requestedDuration || 30 * 60 * 1000,
        status: "PENDING",
        requestedAt: serverTime
    };

    serverRequests.set(requestId, requestMeta);

    // Notify owner via WebSocket if connected
    const ownerWs = connectedClients.get(policy.ownerId);
    if (ownerWs && ownerWs.readyState === 1) {
        ownerWs.send(JSON.stringify({ type: "ACCESS_REQUEST_RECEIVED", data: requestMeta }));
    }

    return res.status(200).json(requestMeta);
});

// 4. Respond to Access Request
app.post('/v1/access-requests/:requestId/respond', (req, res) => {
    const { requestId } = req.params;
    const { responderUserId, approved, finalDurationMillis } = req.body;
    const serverTime = Date.now();

    const reqMeta = serverRequests.get(requestId);
    if (!reqMeta) {
        return res.status(404).json({ error: "Request not found" });
    }

    const policy = serverPolicies.get(reqMeta.messageId);
    if (!policy) {
        return res.status(404).json({ error: "Policy not found" });
    }

    if (policy.ownerId !== responderUserId) {
        return res.status(403).json({ error: "Only content owner can approve/reject access" });
    }

    if (approved) {
        const duration = finalDurationMillis || reqMeta.requestedDuration;
        const maxCeiling = policy.maxExpiresAt || policy.expiresAt;
        const calcExpiry = serverTime + duration;
        const finalExpiresAt = maxCeiling ? Math.min(calcExpiry, maxCeiling) : calcExpiry;

        const grant = {
            grantId: `grant_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`,
            requestId,
            messageId: reqMeta.messageId,
            granteeId: reqMeta.requesterId,
            grantedDuration: Math.max(0, finalExpiresAt - serverTime),
            grantedAt: serverTime,
            expiresAt: finalExpiresAt,
            isRevoked: false
        };

        serverGrants.set(grant.grantId, grant);
        policy.authorizedUserIds.add(reqMeta.requesterId);
        reqMeta.status = "APPROVED";

        // Notify requester via WebSocket
        const requesterWs = connectedClients.get(reqMeta.requesterId);
        if (requesterWs && requesterWs.readyState === 1) {
            requesterWs.send(JSON.stringify({ type: "ACCESS_GRANTED", data: grant }));
        }

        return res.json({ grant });
    } else {
        reqMeta.status = "REJECTED";
        const requesterWs = connectedClients.get(reqMeta.requesterId);
        if (requesterWs && requesterWs.readyState === 1) {
            requesterWs.send(JSON.stringify({ type: "ACCESS_REJECTED", data: { requestId } }));
        }
        return res.json({ success: true, status: "REJECTED" });
    }
});

// 5. Revoke Policy
app.post('/v1/policies/:messageId/revoke', (req, res) => {
    const { messageId } = req.params;
    const { callerUserId } = req.body;

    const policy = serverPolicies.get(messageId);
    if (!policy) return res.status(404).json({ error: "Policy not found" });
    if (policy.ownerId !== callerUserId) return res.status(403).json({ error: "Unauthorized" });

    policy.isRevoked = true;
    policy.authorizedUserIds.clear();

    for (const grant of serverGrants.values()) {
        if (grant.messageId === messageId) {
            grant.isRevoked = true;
        }
    }

    return res.json({ success: true, message: "Policy revoked" });
});

// 6. Sync Expired Content
app.post('/v1/sync/expired', (req, res) => {
    const { messageIds } = req.body;
    const serverTime = Date.now();
    const expired = [];

    if (Array.isArray(messageIds)) {
        for (const id of messageIds) {
            const p = serverPolicies.get(id);
            if (p && (p.isRevoked || (p.expiresAt && serverTime >= p.expiresAt))) {
                expired.push(id);
            }
        }
    }

    return res.json({ expired, serverTime });
});

// --- WebSocket Real-Time Secure Relay ---
wss.on('connection', (ws) => {
    let authenticatedUserId = null;

    ws.on('message', (message) => {
        try {
            const packet = JSON.parse(message);
            if (packet.type === 'IDENTIFY') {
                authenticatedUserId = packet.userId;
                connectedClients.set(authenticatedUserId, ws);
            } else if (packet.type === 'RELAY_ENCRYPTED_MESSAGE') {
                const targetWs = connectedClients.get(packet.toUserId);
                if (targetWs && targetWs.readyState === 1) {
                    targetWs.send(JSON.stringify({
                        type: 'INCOMING_ENCRYPTED_MESSAGE',
                        fromUserId: authenticatedUserId,
                        payload: packet.payload,
                        timestamp: Date.now()
                    }));
                }
            }
        } catch (_) {}
    });

    ws.on('close', () => {
        if (authenticatedUserId) {
            connectedClients.delete(authenticatedUserId);
        }
    });
});

server.listen(PORT, () => {
    console.log(`Cryptora Secure Relay Server listening on port ${PORT}`);
});
