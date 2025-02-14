package app.filemanager.service.rpc

import app.filemanager.data.main.DeviceCategory
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.data.main.DeviceConnectType.*
import app.filemanager.db.FileManagerDatabase
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.RpcClientManager.Companion.CONNECT_TIMEOUT
import app.filemanager.ui.state.main.DeviceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.CoroutineContext

@Rpc
interface DeviceService : RemoteService {
    suspend fun connect(device: SocketDevice): DeviceConnectType
}

class DeviceServiceImpl(override val coroutineContext: CoroutineContext) : DeviceService, KoinComponent {
    private val database by inject<FileManagerDatabase>()
    private val deviceState by inject<DeviceState>()

    override suspend fun connect(device: SocketDevice): DeviceConnectType {
        val queriedDevice =
            database.deviceQueries.queryById(device.id, DeviceCategory.SERVER).executeAsOneOrNull()

        if (queriedDevice != null) {
            when (queriedDevice.connectionType) {
                PERMANENTLY_BANNED,
                AUTO_CONNECT -> {
                    if (queriedDevice.connectionType == PERMANENTLY_BANNED) {
                        return REJECTED
                    }
                    if (queriedDevice.connectionType == AUTO_CONNECT) {
                        return APPROVED
                    }
                    database.deviceQueries.updateConnectionTypeByIdAndCategory(
                        connectionType = queriedDevice.connectionType,
                        id = device.id,
                        category = DeviceCategory.SERVER
                    )
                }

                APPROVED, REJECTED -> {
                    deviceState.connectionRequest[device.id] = Pair(WAITING, Clock.System.now().toEpochMilliseconds())
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
                                    return@withTimeout APPROVED
                                }

                                else -> throw Exception()
                            }
                        }
                    } catch (e: Exception) {
                        deviceState.connectionRequest.remove(device.id)
                        REJECTED
                    }
                }

                else -> {}
            }
            database.deviceQueries.updateLastConnectionByCategoryAndId(device.id, DeviceCategory.SERVER)
        } else {
            deviceState.connectionRequest[device.id] = Pair(WAITING, Clock.System.now().toEpochMilliseconds())
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
                    database.deviceQueries.insert(
                        id = device.id,
                        name = device.name,
                        type = device.type,
                        connectionType = deviceState.connectionRequest[device.id]?.first ?: WAITING,
                        category = DeviceCategory.SERVER,
                        -1
                    )
                    when (deviceState.connectionRequest[device.id]!!.first) {
                        AUTO_CONNECT, APPROVED -> {}
                        else -> throw Exception()
                    }

                    return@withTimeout APPROVED
                }
            } catch (e: Exception) {
                deviceState.connectionRequest.remove(device.id)
                REJECTED
            }
        }

        return REJECTED
    }
}