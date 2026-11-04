package com.example.myapplication.bitchat.mesh

import com.example.myapplication.bitchat.protocol.MeshPacket
import com.example.myapplication.bitchat.protocol.PacketType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections

class BitChatMeshRouter(val myNodeId: String, val myNickname: String) {

    // LRU Cache for deduplication (remembers up to 200 message IDs)
    private val seenMessageIds: MutableSet<String> = Collections.synchronizedSet(
        object : LinkedHashMap<String, Boolean>(200, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
                return size > 200
            }
        }.keys
    )

    // Store & Forward Queue for undelivered offline messages
    private val _offlineQueue = MutableStateFlow<List<MeshPacket>>(emptyList())
    val offlineQueue: StateFlow<List<MeshPacket>> = _offlineQueue.asStateFlow()

    // Active Mesh Peers
    private val _peers = MutableStateFlow<Map<String, PeerNode>>(emptyMap())
    val peers: StateFlow<Map<String, PeerNode>> = _peers.asStateFlow()

    // Callbacks
    var onMessageDelivered: ((MeshPacket) -> Unit)? = null
    var onRelayPacket: ((MeshPacket) -> Unit)? = null

    fun processIncomingPacket(packet: MeshPacket, rssi: Int = -60, address: String? = null) {
        // 1. Deduplication check
        if (!seenMessageIds.add(packet.messageId)) {
            return // Packet already processed, ignore loop
        }

        // 2. Peer presence update
        if (packet.senderId != myNodeId) {
            updatePeer(
                nodeId = packet.senderId,
                nickname = packet.payload.split("|").getOrNull(0) ?: packet.senderId,
                rssi = rssi,
                lat = packet.latitude,
                lon = packet.longitude,
                geohash = packet.geohash,
                hopCount = packet.hopCount + 1,
                address = address
            )
        }

        // 3. Check if message is intended for this node or is a broadcast
        val isForMe = packet.recipientId == myNodeId || packet.recipientId == MeshPacket.BROADCAST_ID
        if (isForMe) {
            onMessageDelivered?.invoke(packet)
        }

        // 4. Multi-hop Relay check
        if (packet.recipientId != myNodeId && packet.hopCount < packet.maxHops) {
            val relayedPacket = packet.copy(hopCount = packet.hopCount + 1)
            onRelayPacket?.invoke(relayedPacket)
        }
    }

    fun queueOfflineMessage(packet: MeshPacket) {
        _offlineQueue.value += packet
    }

    fun flushOfflineQueueForPeer(peerId: String, sendBlock: (MeshPacket) -> Unit) {
        val remaining = mutableListOf<MeshPacket>()
        for (packet in _offlineQueue.value) {
            if (packet.recipientId == peerId || packet.recipientId == MeshPacket.BROADCAST_ID) {
                sendBlock(packet)
            } else {
                remaining.add(packet)
            }
        }
        _offlineQueue.value = remaining
    }

    fun updatePeer(
        nodeId: String,
        nickname: String,
        rssi: Int,
        lat: Double?,
        lon: Double?,
        geohash: String?,
        hopCount: Int,
        address: String?
    ) {
        val currentPeers = _peers.value.toMutableMap()
        val estimatedDist = calculateRssiDistance(rssi)
        val updated = PeerNode(
            nodeId = nodeId,
            nickname = nickname,
            rssi = rssi,
            estimatedDistanceMeters = estimatedDist,
            latitude = lat,
            longitude = lon,
            geohash = geohash,
            lastSeenTimestamp = System.currentTimeMillis(),
            hopCount = hopCount,
            address = address
        )
        currentPeers[nodeId] = updated
        _peers.value = currentPeers
    }

    private fun calculateRssiDistance(rssi: Int): Double {
        // Path-loss model formula: d = 10^((MeasuredPower - RSSI) / (10 * N))
        // MeasuredPower = -69 dBm at 1m, N = 2.0 (free space)
        val measuredPower = -69
        val n = 2.0
        return Math.pow(10.0, (measuredPower - rssi) / (10.0 * n))
    }

    fun createAnnouncementPacket(
        myLat: Double? = null,
        myLon: Double? = null,
        myGeohash: String? = null
    ): MeshPacket {
        return MeshPacket(
            packetType = PacketType.PEER_ANNOUNCE,
            senderId = myNodeId,
            recipientId = MeshPacket.BROADCAST_ID,
            latitude = myLat,
            longitude = myLon,
            geohash = myGeohash,
            payload = myNickname
        )
    }

    fun pruneStalePeers(timeoutMs: Long = 60_000) {
        val now = System.currentTimeMillis()
        val active = _peers.value.filter { now - it.value.lastSeenTimestamp < timeoutMs }
        _peers.value = active
    }
}
