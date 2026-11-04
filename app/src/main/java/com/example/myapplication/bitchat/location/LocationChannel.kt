package com.example.myapplication.bitchat.location

data class LocationChannel(
    val channelId: String,
    val name: String,
    val precision: Int,
    val geohash: String,
    val activePeerCount: Int = 0
)
