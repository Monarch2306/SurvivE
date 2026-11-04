package com.example.myapplication.bitchat.mesh

data class PeerNode(
    val nodeId: String,
    val nickname: String,
    val rssi: Int = -60,
    val estimatedDistanceMeters: Double = 5.0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geohash: String? = null,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val hopCount: Int = 1,
    val address: String? = null
) {
    val isDirectNeighbor: Boolean
        get() = hopCount == 1
}
