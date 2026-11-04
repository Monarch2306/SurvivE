package com.example.myapplication.bitchat.protocol

enum class PacketType(val code: Byte) {
    TEXT(0x01),
    VOICE(0x02),
    PEER_ANNOUNCE(0x03),
    ACK(0x04),
    ROUTE_DISCOVERY(0x05);

    companion object {
        fun fromCode(code: Byte): PacketType {
            return entries.find { it.code == code } ?: TEXT
        }
    }
}
