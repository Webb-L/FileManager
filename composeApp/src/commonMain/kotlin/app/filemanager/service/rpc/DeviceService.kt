package app.filemanager.service.rpc

import app.filemanager.data.main.DeviceCategory
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.data.main.DeviceConnectType.*
import app.filemanager.db.FileManagerDatabase
import app.filemanager.service.BaseSocketManager.Companion.CONNECT_TIMEOUT
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.SocketDevice
import app.filemanager.ui.state.main.DeviceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
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

    override suspend fun connect(socketDevice: SocketDevice): DeviceConnectType {
        val queriedDevice =
            database.deviceQueries.queryById(socketDevice.id, DeviceCategory.SERVER).executeAsOneOrNull()

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
                        id = socketDevice.id,
                        category = DeviceCategory.SERVER
                    )
                }

                APPROVED, REJECTED -> {
                    deviceState.connectionRequest[socketDevice.id] = WAITING
                    val index = deviceState.socketDevices.indexOfFirst { it.id == socketDevice.id }
                    if (index >= 0) {
                        deviceState.socketDevices[index] =
                            socketDevice.withCopy(
                                connectType = ConnectType.Loading
                            )
                    } else {
                        deviceState.socketDevices.add(
                            socketDevice.withCopy(
                                connectType = ConnectType.Loading
                            )
                        )
                    }
                    return try {
                        withTimeout(CONNECT_TIMEOUT * 1000L) {
                            while (deviceState.connectionRequest[socketDevice.id] == WAITING) {
                                delay(300L)
                            }
                            when (deviceState.connectionRequest[socketDevice.id]) {
                                AUTO_CONNECT, APPROVED -> {
                                    return@withTimeout APPROVED
                                }

                                else -> throw Exception()
                            }
                        }
                    } catch (e: Exception) {
                        deviceState.connectionRequest.remove(socketDevice.id)
                        REJECTED
                    }
                }

                else -> {}
            }
            database.deviceQueries.updateLastConnectionByCategoryAndId(socketDevice.id, DeviceCategory.SERVER)
        } else {
            deviceState.connectionRequest[socketDevice.id] = WAITING
            val index = deviceState.socketDevices.indexOfFirst { it.id == socketDevice.id }
            if (index >= 0) {
                deviceState.socketDevices[index] =
                    socketDevice.withCopy(
                        connectType = ConnectType.Loading
                    )
            } else {
                deviceState.socketDevices.add(
                    socketDevice.withCopy(
                        connectType = ConnectType.Loading
                    )
                )
            }
            return try {
                withTimeout(CONNECT_TIMEOUT * 1000L) {
                    while (deviceState.connectionRequest[socketDevice.id] == WAITING) {
                        delay(300L)
                    }
                    database.deviceQueries.insert(
                        id = socketDevice.id,
                        name = socketDevice.name,
                        type = socketDevice.type,
                        connectionType = deviceState.connectionRequest[socketDevice.id] ?: WAITING,
                        category = DeviceCategory.SERVER
                    )
                    when (deviceState.connectionRequest[socketDevice.id]) {
                        AUTO_CONNECT, APPROVED -> {}
                        else -> throw Exception()
                    }

                    return@withTimeout APPROVED
                }
            } catch (e: Exception) {
                deviceState.connectionRequest.remove(socketDevice.id)
                REJECTED
            }
        }

        return REJECTED
    }
}