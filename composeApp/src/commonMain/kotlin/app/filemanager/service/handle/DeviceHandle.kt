package app.filemanager.service.handle

import app.filemanager.data.main.DeviceCategory
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.db.FileManagerDatabase
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.DeviceService
import app.filemanager.service.rpc.RpcClientManager
import app.filemanager.ui.state.main.DeviceState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeviceHandle(private val deviceService: DeviceService) : KoinComponent {
    private val deviceState by inject<DeviceState>()
    private val database by inject<FileManagerDatabase>()

    suspend fun connect(rpc: RpcClientManager, connectDevice: SocketDevice) {
        // TODO 将我当前的设备发送，而不是将连接设备发送
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
                if (database.deviceQueries.queryById(socketDevice.id, DeviceCategory.CLIENT)
                        .executeAsOneOrNull() != null
                ) {
                    return
                }
                database.deviceQueries.insert(
                    id = socketDevice.id,
                    name = socketDevice.name,
                    type = socketDevice.type,
                    connectionType = DeviceConnectType.APPROVED,
                    category = DeviceCategory.CLIENT
                )
            }
        }
    }
}