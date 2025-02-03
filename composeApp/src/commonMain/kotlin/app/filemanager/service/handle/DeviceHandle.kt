package app.filemanager.service.handle

import app.filemanager.data.main.DeviceConnectType
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.DeviceService
import app.filemanager.service.rpc.RpcClientManager
import app.filemanager.ui.state.main.DeviceState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeviceHandle(private val deviceService: DeviceService) : KoinComponent {
    private val deviceState by inject<DeviceState>()

    suspend fun connect(rpc: RpcClientManager, connectDevice: SocketDevice) {
        val connectType = deviceService.connect(connectDevice)
        deviceState.socketDevices.indexOfFirst { it.id == connectDevice.id }.takeIf { it >= 0 }?.let { index ->
            val socketDevice = connectDevice.withCopy(
                connectType = if (connectType == DeviceConnectType.APPROVED)
                    ConnectType.Connect
                else
                    ConnectType.Fail,
                client = rpc
            )
            deviceState.socketDevices[index] = socketDevice

            if (socketDevice.connectType == ConnectType.Connect) {
                deviceState.devices.add(socketDevice.toDevice())
            }
        }
    }
}