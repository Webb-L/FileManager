package app.filemanager.service.handle

import app.filemanager.PlatformType
import app.filemanager.createSettings
import app.filemanager.data.main.DeviceCategory
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.db.FileManagerDatabase
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.RpcClientManager
import app.filemanager.service.rpc.SocketClientIPEnum
import app.filemanager.service.rpc.getAllIPAddresses
import app.filemanager.ui.state.main.DeviceState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeviceHandle(private val rpc: RpcClientManager) : KoinComponent {
    private val deviceState by inject<DeviceState>()
    private val database by inject<FileManagerDatabase>()
    private val settings = createSettings()


    suspend fun connect(rpc: RpcClientManager, connectDevice: SocketDevice): String {
        val connectType = rpc.deviceService.connect(
            SocketDevice(
                id = settings.getString("deviceId", ""),
                name = settings.getString("deviceName", ""),
                host = getAllIPAddresses(type = SocketClientIPEnum.IPV4_UP).firstOrNull() ?: "",
                type = PlatformType,
                connectType = ConnectType.UnConnect
            )
        )
        deviceState.socketDevices.indexOfFirst { it.id == connectDevice.id }.takeIf { it >= 0 }?.let { index ->
            val socketDevice = connectDevice.withCopy(
                connectType = if (connectType.first == DeviceConnectType.APPROVED)
                    ConnectType.Connect
                else
                    ConnectType.Fail,
                token = connectType.second,
                client = rpc
            )
            deviceState.socketDevices[index] = socketDevice

            if (socketDevice.connectType == ConnectType.Connect) {
                deviceState.devices.add(socketDevice.toDevice())
                if (database.deviceQueries.queryById(socketDevice.id, DeviceCategory.CLIENT)
                        .executeAsOneOrNull() != null
                ) {
                    return connectType.second
                }
            }
        }

        return ""
    }
}