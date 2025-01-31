package app.filemanager.ui.state.main

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.service.SocketClientManger
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.SocketDevice
import io.ktor.util.network.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeviceState : KoinComponent {
    private val mainState by inject<MainState>()
    val socketClientManger = SocketClientManger()
    private val mainScope = MainScope()

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

    // TODO 获取断开的链接
    suspend fun scanner(ips: List<String>) {
        updateLoadingDevices(true)
        socketClientManger.socket.scanner(ips) { socketDevice ->
            if (socketDevice.connectType == ConnectType.Loading) {
                connect(socketDevice)
            }
            socketDevices.add(socketDevice)
        }
        updateLoadingDevices(false)
    }

    fun connect(connectDevice: SocketDevice) {
        mainScope.launch {
            try {
                socketClientManger.connect(connectDevice)
            } catch (e: UnresolvedAddressException) {
                println(e)
                socketDevices.indexOfFirst { it.id == connectDevice.id }.takeIf { it >= 0 }?.let { index ->
                    socketDevices[index] = connectDevice.withCopy(
                        connectType = ConnectType.Fail
                    )
                }
            } catch (e: Exception) {
                println(e)
            }
        }
    }
}