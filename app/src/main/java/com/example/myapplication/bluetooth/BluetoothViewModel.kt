package com.example.myapplication.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.bitchat.mesh.PeerNode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@SuppressLint("MissingPermission")
class BluetoothViewModel(private val context: Context, private val adapter: BluetoothAdapter?) : ViewModel() {
    val service = BluetoothService(context, adapter)

    val myNodeId: String = service.myNodeId
    val myNickname: String = service.myNickname

    private val _isBluetoothEnabled = MutableStateFlow(service.isBluetoothEnabled())
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _pairedDevices = MutableStateFlow<Set<BluetoothDevice>>(emptySet())
    val pairedDevices: StateFlow<Set<BluetoothDevice>> = _pairedDevices.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<Set<BluetoothDevice>>(emptySet())
    val discoveredDevices: StateFlow<Set<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    val meshPeers: StateFlow<List<PeerNode>> = service.meshRouter.peers
        .map { it.values.toList().sortedBy { peer -> peer.hopCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineQueueCount: StateFlow<Int> = service.meshRouter.offlineQueue
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        service.onConnectionStateChanged = { _isConnected.value = it }
        service.onMessageReceived = { msg -> _messages.value += msg }
        service.onDeviceFound = { device ->
            _discoveredDevices.value += device
        }
        service.onDiscoveryStarted = { _isScanning.value = true }
        service.onDiscoveryFinished = { _isScanning.value = false }
    }

    fun checkBluetoothState() {
        val enabled = service.isBluetoothEnabled()
        _isBluetoothEnabled.value = enabled
        if (enabled) {
            updatePairedDevices()
        } else {
            _pairedDevices.value = emptySet()
            _discoveredDevices.value = emptySet()
            _isScanning.value = false
        }
    }

    fun updateLocation(lat: Double?, lon: Double?, geohash: String?) {
        service.updateLocation(lat, lon, geohash)
    }

    fun updatePairedDevices() {
        try {
            if (service.isBluetoothEnabled()) {
                _pairedDevices.value = adapter?.bondedDevices ?: emptySet()
            } else {
                _pairedDevices.value = emptySet()
            }
        } catch (e: Exception) {
            _pairedDevices.value = emptySet()
        }
    }

    fun startDiscovery() {
        if (!service.isBluetoothEnabled()) return
        _discoveredDevices.value = emptySet()
        service.startDiscovery()
    }

    fun stopDiscovery() {
        service.stopDiscovery()
    }

    fun connectToDevice(device: BluetoothDevice) {
        if (!service.isBluetoothEnabled()) return
        service.connectToDevice(device)
    }

    fun disconnect() {
        service.stopAll()
    }

    fun startServer() {
        if (!service.isBluetoothEnabled()) return
        service.startServer()
    }

    fun sendMessage(message: String, recipientId: String = "BROADCAST") {
        service.sendMessage(message, recipientId)
        _messages.value += "Me: $message"
    }

    fun startVoice(start: Boolean) {
        service.startVoice(start)
    }

    override fun onCleared() {
        super.onCleared()
        service.stopAll()
    }
}
