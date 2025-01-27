package app.filemanager.ui.state.main

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.service.data.SocketDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DeviceState() {
    private val _isDeviceAdd: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isDeviceAdd: StateFlow<Boolean> = _isDeviceAdd
    fun updateDeviceAdd(value: Boolean) {
        _isDeviceAdd.value = value
    }

    val devices = mutableStateListOf<Device>()

    val socketDevices = mutableStateListOf<SocketDevice>()

    private val _loadingDevices: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val loadingDevices: StateFlow<Boolean> = _loadingDevices
    fun updateLoadingDevices(value: Boolean) {
        _loadingDevices.value = value
    }


    val connectionRequest = mutableStateMapOf<String, DeviceConnectType>()
}