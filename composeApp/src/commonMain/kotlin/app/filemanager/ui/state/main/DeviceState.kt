package app.filemanager.ui.state.main

import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceType
import app.filemanager.service.WebSocketConnectService
import io.ktor.utils.io.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class DeviceState() {
    private val _isDeviceAdd: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isDeviceAdd: StateFlow<Boolean> = _isDeviceAdd
    fun updateDeviceAdd(value: Boolean) {
        _isDeviceAdd.value = value
    }

    private val _devices: MutableStateFlow<List<Device>> = MutableStateFlow(listOf())
    val devices: StateFlow<List<Device>> = _devices
    @OptIn(ExperimentalEncodingApi::class)
    fun addDevices(message: String, connect: WebSocketConnectService) {
        val line = message.split(" ")
        val deviceId = line[0]
        val deviceName = String(Base64.decode(line[1].toByteArray()))
        val deviceHost = line[2]

        _devices.value = mutableListOf<Device>().apply {
            addAll(_devices.value)
            val index = _devices.value.indexOfFirst { it.id === deviceId }
            if (index < 0) {
                add(
                    Device(
                        id = deviceId,
                        name = deviceName,
                        host = mutableMapOf(deviceHost to connect),
                        type = DeviceType.IOS
                    )
                )
            } else {
                _devices.value[index].host[deviceHost] = connect
            }
        }
    }
}