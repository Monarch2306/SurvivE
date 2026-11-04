package com.example.myapplication.bitchat.protocol

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

data class MeshPacket(
    val magicByte: Byte = MAGIC_BYTE,
    val version: Byte = 1,
    val packetType: PacketType,
    val senderId: String,
    val recipientId: String = BROADCAST_ID,
    val messageId: String = UUID.randomUUID().toString().take(8),
    var hopCount: Int = 0,
    val maxHops: Int = MAX_HOPS_DEFAULT,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geohash: String? = null,
    val payload: String
) {
    fun toByteArray(): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        dos.writeByte(magicByte.toInt())
        dos.writeByte(version.toInt())
        dos.writeByte(packetType.code.toInt())
        dos.writeUTF(senderId)
        dos.writeUTF(recipientId)
        dos.writeUTF(messageId)
        dos.writeInt(hopCount)
        dos.writeInt(maxHops)
        dos.writeLong(timestamp)
        dos.writeDouble(latitude ?: 0.0)
        dos.writeDouble(longitude ?: 0.0)
        dos.writeUTF(geohash ?: "")
        dos.writeUTF(payload)

        dos.flush()
        return baos.toByteArray()
    }

    companion object {
        const val MAGIC_BYTE: Byte = 0xBC.toByte()
        const val BROADCAST_ID: String = "BROADCAST"
        const val MAX_HOPS_DEFAULT: Int = 7

        fun fromByteArray(bytes: ByteArray): MeshPacket? {
            return try {
                val bais = ByteArrayInputStream(bytes)
                val dis = DataInputStream(bais)

                val magic = dis.readByte()
                if (magic != MAGIC_BYTE) return null

                val version = dis.readByte()
                val packetTypeCode = dis.readByte()
                val packetType = PacketType.fromCode(packetTypeCode)
                val senderId = dis.readUTF()
                val recipientId = dis.readUTF()
                val messageId = dis.readUTF()
                val hopCount = dis.readInt()
                val maxHops = dis.readInt()
                val timestamp = dis.readLong()
                val lat = dis.readDouble()
                val lon = dis.readDouble()
                val geohash = dis.readUTF()
                val payload = dis.readUTF()

                MeshPacket(
                    magicByte = magic,
                    version = version,
                    packetType = packetType,
                    senderId = senderId,
                    recipientId = recipientId,
                    messageId = messageId,
                    hopCount = hopCount,
                    maxHops = maxHops,
                    timestamp = timestamp,
                    latitude = if (lat == 0.0) null else lat,
                    longitude = if (lon == 0.0) null else lon,
                    geohash = geohash.ifEmpty { null },
                    payload = payload
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
