package app.filemanager.service.rpc

import app.filemanager.data.main.DeviceCategory
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.data.main.DeviceConnectType.*
import app.filemanager.db.FileManagerDatabase
import app.filemanager.extensions.randomString
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.RpcClientManager.Companion.CONNECT_TIMEOUT
import app.filemanager.ui.state.device.DeviceCertificateState
import app.filemanager.ui.state.main.DeviceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.rpc.annotations.Rpc
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Rpc
interface DeviceService {
    suspend fun connect(device: SocketDevice): Pair<DeviceConnectType, String>
}

class DeviceServiceImpl() : DeviceService, KoinComponent {
    private val database by inject<FileManagerDatabase>()
    private val deviceState by inject<DeviceState>()
    private val deviceCertificateState by inject<DeviceCertificateState>()

    override suspend fun connect(device: SocketDevice): Pair<DeviceConnectType, String> {
        val queriedDevice =
            database.deviceConnectQueries.queryByIdAndCategory(device.id, DeviceCategory.SERVER).executeAsOneOrNull()

        val randomString = 32.randomString()
        if (queriedDevice != null) {
            when (queriedDevice.connectionType) {
                PERMANENTLY_BANNED,
                AUTO_CONNECT -> {
                    if (queriedDevice.connectionType == PERMANENTLY_BANNED) {
                        return Pair(REJECTED, "")
                    }
                    if (queriedDevice.connectionType == AUTO_CONNECT) {
                        deviceCertificateState.permissions[randomString] = queriedDevice.roleId
                        return Pair(APPROVED, randomString)
                    }
                    database.deviceConnectQueries.updateConnectionTypeByIdAndCategory(
                        connectionType = queriedDevice.connectionType,
                        id = device.id,
                        category = DeviceCategory.SERVER
                    )
                }

                APPROVED, REJECTED -> {
                    deviceState.connectionRequest[device.id] = Pair(WAITING, System.currentTimeMillis())
                    if (deviceState.socketDevices.firstOrNull { it.id == device.id } == null) {
                        deviceState.socketDevices.add(
                            device.withCopy(
                                connectType = ConnectType.UnConnect
                            )
                        )
                    }
                    return try {
                        withTimeout(CONNECT_TIMEOUT * 1000L) {
                            while (deviceState.connectionRequest[device.id]!!.first == WAITING) {
                                delay(300L)
                            }
                            when (deviceState.connectionRequest[device.id]!!.first) {
                                AUTO_CONNECT, APPROVED -> {
                                    deviceCertificateState.permissions[randomString] = queriedDevice.roleId
                                    return@withTimeout Pair(APPROVED, randomString)
                                }

                                else -> throw Exception()
                            }
                        }
                    } catch (e: Exception) {
                        deviceState.connectionRequest.remove(device.id)
                        Pair(REJECTED, "")
                    }
                }

                else -> {}
            }
            database.deviceConnectQueries.updateLastConnectionByCategoryAndId(device.id, DeviceCategory.SERVER)
        } else {
            deviceState.connectionRequest[device.id] = Pair(WAITING, System.currentTimeMillis())
            if (deviceState.socketDevices.firstOrNull { it.id == device.id } == null) {
                deviceState.socketDevices.add(
                    device.withCopy(
                        connectType = ConnectType.New
                    )
                )
            }
            return try {
                withTimeout(CONNECT_TIMEOUT * 1000L) {
                    while (deviceState.connectionRequest[device.id]!!.first == WAITING) {
                        delay(300L)
                    }
                    when (deviceState.connectionRequest[device.id]!!.first) {
                        AUTO_CONNECT, APPROVED -> {}
                        else -> throw Exception()
                    }
                    deviceCertificateState.permissions[randomString] = -1
                    return@withTimeout Pair(APPROVED, randomString)
                }
            } catch (e: Exception) {
                deviceState.connectionRequest.remove(device.id)
                Pair(REJECTED, "")
            }
        }

        return Pair(REJECTED, "")
    }
}