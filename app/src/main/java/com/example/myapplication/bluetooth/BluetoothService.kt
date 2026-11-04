package com.example.myapplication.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import com.example.myapplication.bitchat.mesh.BitChatMeshRouter
import com.example.myapplication.bitchat.protocol.MeshPacket
import com.example.myapplication.bitchat.protocol.PacketType
import kotlinx.coroutines.*
import java.io.InputStream
import java.util.*

@SuppressLint("MissingPermission")
class BluetoothService(private val context: Context, private val adapter: BluetoothAdapter?) {
    private val NAME = "SurviveApp"
    private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    val myNodeId: String = (Build.MODEL.take(4) + "-" + UUID.randomUUID().toString().take(4)).uppercase()
    val myNickname: String = Build.MODEL ?: "Node-$myNodeId"

    val meshRouter = BitChatMeshRouter(myNodeId, myNickname)

    private var serverJob: Job? = null
    private var clientJob: Job? = null
    private var audioSendJob: Job? = null
    private var listeningJob: Job? = null
    private var heartbeatJob: Job? = null

    var onMessageReceived: ((String) -> Unit)? = null
    var onMeshPacketReceived: ((MeshPacket) -> Unit)? = null
    var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    var onDeviceFound: ((BluetoothDevice) -> Unit)? = null
    var onDiscoveryStarted: (() -> Unit)? = null
    var onDiscoveryFinished: (() -> Unit)? = null

    private var currentSocket: BluetoothSocket? = null
    private var isRecording = false
    private var rxAudioTrack: AudioTrack? = null

    private var currentLat: Double? = null
    private var currentLon: Double? = null
    private var currentGeohash: String? = null

    init {
        meshRouter.onMessageDelivered = { packet ->
            onMeshPacketReceived?.invoke(packet)
            when (packet.packetType) {
                PacketType.TEXT -> {
                    onMessageReceived?.invoke("[${packet.senderId}${if (packet.hopCount > 1) " via ${packet.hopCount} hops" else ""}]: ${packet.payload}")
                }
                PacketType.VOICE -> {
                    onMessageReceived?.invoke("[Voice Stream from ${packet.senderId}]")
                    playIncomingVoicePacket(packet)
                }
                else -> {}
            }
        }

        meshRouter.onRelayPacket = { packet ->
            sendMeshPacket(packet)
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return try {
            adapter != null && adapter.isEnabled
        } catch (e: Exception) {
            false
        }
    }

    fun updateLocation(lat: Double?, lon: Double?, geohash: String?) {
        currentLat = lat
        currentLon = lon
        currentGeohash = geohash
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    device?.let {
                        meshRouter.updatePeer(
                            nodeId = it.address,
                            nickname = it.name ?: it.address,
                            rssi = if (rssi != Short.MIN_VALUE.toInt()) rssi else -65,
                            lat = null,
                            lon = null,
                            geohash = null,
                            hopCount = 1,
                            address = it.address
                        )
                        onDeviceFound?.invoke(it)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    onDiscoveryStarted?.invoke()
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    onDiscoveryFinished?.invoke()
                }
            }
        }
    }

    fun startDiscovery() {
        if (!isBluetoothEnabled()) return
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            context.registerReceiver(discoveryReceiver, filter)
            adapter?.startDiscovery()
        } catch (e: Exception) {
            onDiscoveryFinished?.invoke()
        }
    }

    fun stopDiscovery() {
        try {
            context.unregisterReceiver(discoveryReceiver)
        } catch (e: Exception) {}
        try {
            adapter?.cancelDiscovery()
        } catch (e: Exception) {}
        onDiscoveryFinished?.invoke()
    }

    fun startServer() {
        if (!isBluetoothEnabled()) return

        serverJob?.cancel()
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val serverSocket: BluetoothServerSocket? = adapter?.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
                while (isActive) {
                    val socket: BluetoothSocket? = try {
                        serverSocket?.accept()
                    } catch (e: Exception) {
                        null
                    }
                    socket?.let {
                        try { serverSocket?.close() } catch (e: Exception) {}
                        manageConnectedSocket(it)
                        return@launch
                    }
                }
            } catch (e: Exception) {
                // Ignore Bluetooth server bind failures gracefully
            }
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        if (!isBluetoothEnabled()) return

        clientJob?.cancel()
        clientJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket: BluetoothSocket? = device.createRfcommSocketToServiceRecord(MY_UUID)
                stopDiscovery()
                socket?.connect()
                socket?.let { manageConnectedSocket(it) }
            } catch (e: Exception) {
                // Connection failed gracefully
            }
        }
    }

    private fun manageConnectedSocket(socket: BluetoothSocket) {
        currentSocket = socket
        onConnectionStateChanged?.invoke(true)
        listenForData(socket)
        startHeartbeat()

        val remoteAddress = socket.remoteDevice?.address ?: "BROADCAST"
        meshRouter.flushOfflineQueueForPeer(remoteAddress) { packet ->
            sendMeshPacket(packet)
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && currentSocket != null) {
                val announce = meshRouter.createAnnouncementPacket(currentLat, currentLon, currentGeohash)
                sendMeshPacket(announce)
                meshRouter.pruneStalePeers()
                delay(12_000)
            }
        }
    }

    private fun listenForData(socket: BluetoothSocket) {
        listeningJob = CoroutineScope(Dispatchers.IO).launch {
            val inputStream: InputStream = socket.inputStream
            val buffer = ByteArray(4096)
            while (isActive) {
                try {
                    val bytes = inputStream.read(buffer)
                    if (bytes == -1) break
                    val actualBytes = buffer.copyOfRange(0, bytes)

                    val packet = MeshPacket.fromByteArray(actualBytes)
                    if (packet != null) {
                        meshRouter.processIncomingPacket(packet, rssi = -60, address = socket.remoteDevice?.address)
                    } else {
                        val text = String(actualBytes)
                        val fallbackPacket = MeshPacket(
                            packetType = PacketType.TEXT,
                            senderId = socket.remoteDevice?.name ?: "Peer",
                            payload = text
                        )
                        meshRouter.processIncomingPacket(fallbackPacket, rssi = -60, address = socket.remoteDevice?.address)
                    }
                } catch (e: Exception) {
                    onConnectionStateChanged?.invoke(false)
                    break
                }
            }
        }
    }

    fun sendMeshPacket(packet: MeshPacket) {
        CoroutineScope(Dispatchers.IO).launch {
            val socket = currentSocket
            if (socket != null && socket.isConnected) {
                try {
                    val bytes = packet.toByteArray()
                    socket.outputStream.write(bytes)
                    socket.outputStream.flush()
                } catch (e: Exception) {
                    meshRouter.queueOfflineMessage(packet)
                }
            } else {
                meshRouter.queueOfflineMessage(packet)
            }
        }
    }

    fun sendMessage(message: String, recipientId: String = MeshPacket.BROADCAST_ID) {
        val packet = MeshPacket(
            packetType = PacketType.TEXT,
            senderId = myNodeId,
            recipientId = recipientId,
            latitude = currentLat,
            longitude = currentLon,
            geohash = currentGeohash,
            payload = message
        )
        sendMeshPacket(packet)
    }

    fun startVoice(start: Boolean) {
        isRecording = start
        if (start) {
            audioSendJob = CoroutineScope(Dispatchers.IO).launch {
                sendAudio()
            }
        } else {
            audioSendJob?.cancel()
        }
    }

    private fun sendAudio() {
        val sampleRate = 8000
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = if (minBufferSize > 0) minBufferSize.coerceAtLeast(1024) else 1024

        var recorder: AudioRecord? = null
        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            val pcmBuffer = ByteArray(1024)
            recorder.startRecording()

            while (isRecording && audioSendJob?.isActive == true) {
                val read = recorder.read(pcmBuffer, 0, pcmBuffer.size)
                if (read > 0) {
                    val validBytes = pcmBuffer.copyOfRange(0, read)
                    val base64Payload = Base64.encodeToString(validBytes, Base64.NO_WRAP)

                    val voicePacket = MeshPacket(
                        packetType = PacketType.VOICE,
                        senderId = myNodeId,
                        recipientId = MeshPacket.BROADCAST_ID,
                        latitude = currentLat,
                        longitude = currentLon,
                        geohash = currentGeohash,
                        payload = base64Payload
                    )

                    sendMeshPacket(voicePacket)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (_: Exception) {}
        }
    }

    private fun playIncomingVoicePacket(packet: MeshPacket) {
        try {
            val pcmBytes = Base64.decode(packet.payload, Base64.NO_WRAP)
            if (pcmBytes.isEmpty()) return

            if (rxAudioTrack == null || rxAudioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                val sampleRate = 8000
                val minBuffer = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                rxAudioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuffer)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                rxAudioTrack?.play()
            }

            rxAudioTrack?.write(pcmBytes, 0, pcmBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAll() {
        stopDiscovery()
        isRecording = false
        heartbeatJob?.cancel()
        serverJob?.cancel()
        clientJob?.cancel()
        audioSendJob?.cancel()
        listeningJob?.cancel()

        try {
            rxAudioTrack?.stop()
            rxAudioTrack?.release()
        } catch (_: Exception) {}
        rxAudioTrack = null

        try {
            currentSocket?.close()
        } catch (e: Exception) {}
        currentSocket = null
        onConnectionStateChanged?.invoke(false)
    }
}
