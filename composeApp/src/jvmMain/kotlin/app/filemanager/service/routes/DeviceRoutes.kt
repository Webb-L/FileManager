package app.filemanager.service.routes

import app.filemanager.data.main.DeviceCategory
import app.filemanager.data.main.DeviceConnectType.*
import app.filemanager.db.FileManagerDatabase
import app.filemanager.extensions.randomString
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.DeviceConnectRequest
import app.filemanager.service.data.DeviceConnectResponse
import app.filemanager.service.plugins.ProtobufRequest
import app.filemanager.service.plugins.receiveProtobuf
import app.filemanager.service.plugins.respondProtobuf
import app.filemanager.service.rpc.HttpRouteClientManager.Companion.CONNECT_TIMEOUT
import app.filemanager.ui.state.device.DeviceCertificateState
import app.filemanager.ui.state.main.DeviceState
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@OptIn(ExperimentalSerializationApi::class)
fun Route.deviceRoutes(): KoinComponent {
    return object : KoinComponent {
        private val database by inject<FileManagerDatabase>()
        private val deviceState by inject<DeviceState>()
        private val deviceCertificateState by inject<DeviceCertificateState>()

        init {
            route("/api/devices") {
                install(ProtobufRequest)

                post("/connect") {
                    try {
                        // 从请求体获取设备连接请求
                        val request = call.receiveProtobuf<DeviceConnectRequest>()
                        val device = request.device

                        val queriedDevice = database.deviceConnectQueries
                            .queryByIdAndCategory(device.id, DeviceCategory.SERVER)
                            .executeAsOneOrNull()

                        val randomString = 32.randomString()

                        if (queriedDevice != null) {
                            when (queriedDevice.connectionType) {
                                PERMANENTLY_BANNED -> {
                                    call.respondProtobuf(DeviceConnectResponse(REJECTED, ""))
                                    return@post
                                }

                                AUTO_CONNECT -> {
                                    deviceCertificateState.setDeviceTokenAndPermission(
                                        device.id,
                                        randomString,
                                        queriedDevice.roleId,
                                    )
                                    call.respondProtobuf(DeviceConnectResponse(APPROVED, randomString))
                                    return@post
                                }

                                APPROVED, REJECTED -> {
                                    deviceState.connectionRequest[device.id] = Pair(WAITING, System.currentTimeMillis())
                                    if (deviceState.socketDevices.firstOrNull { it.id == device.id } == null) {
                                        deviceState.socketDevices.add(
                                            device.withCopy(
                                                connectType = ConnectType.UnConnect,
                                            )
                                        )
                                    }

                                    try {
                                        val result = withTimeout(CONNECT_TIMEOUT * 1000L) {
                                            while (deviceState.connectionRequest[device.id]!!.first == WAITING) {
                                                delay(1000L)
                                            }

                                            when (deviceState.connectionRequest[device.id]!!.first) {
                                                AUTO_CONNECT, APPROVED -> {
                                                    deviceCertificateState.setDeviceTokenAndPermission(
                                                        device.id,
                                                        randomString,
                                                        queriedDevice.roleId,
                                                    )
                                                    DeviceConnectResponse(APPROVED, randomString)
                                                }

                                                else -> throw Exception("Connection rejected")
                                            }
                                        }
                                        call.respondProtobuf(result)
                                    } catch (e: Exception) {
                                        deviceState.connectionRequest.remove(device.id)
                                        call.respondProtobuf(DeviceConnectResponse(REJECTED, ""))
                                    }
                                }

                                else -> {}
                            }
                            database.deviceConnectQueries.updateLastConnectionByCategoryAndId(
                                device.id,
                                DeviceCategory.SERVER
                            )
                        } else {
                            deviceState.connectionRequest[device.id] = Pair(WAITING, System.currentTimeMillis())
                            if (deviceState.socketDevices.firstOrNull { it.id == device.id } == null) {
                                val deviceData = database.deviceQueries.queryById(device.id).executeAsOneOrNull()
                                if (deviceData == null) {
                                    database.deviceQueries.insert(
                                        id = device.id,
                                        name = device.name,
                                        host = device.host,
                                        port = device.port.toLong(),
                                        type = device.type
                                    )
                                }

                                deviceState.socketDevices.add(
                                    device.withCopy(
                                        connectType = ConnectType.New,
                                    )
                                )
                            }

                            try {
                                val result = withTimeout(CONNECT_TIMEOUT * 1000L) {
                                    while (deviceState.connectionRequest[device.id]!!.first == WAITING) {
                                        delay(300L)
                                    }

                                    val roleId = when (deviceState.connectionRequest[device.id]!!.first) {
                                        AUTO_CONNECT, APPROVED -> {
                                            database.deviceConnectQueries.queryRoleIdByIdAndCategory(
                                                device.id,
                                                DeviceCategory.SERVER,
                                            ).executeAsOneOrNull() ?: -1
                                        }

                                        else -> throw Exception("Connection rejected")
                                    }

                                    deviceCertificateState.setDeviceTokenAndPermission(
                                        device.id,
                                        randomString,
                                        roleId,
                                    )
                                    DeviceConnectResponse(APPROVED, randomString)
                                }
                                call.respondProtobuf(result)
                            } catch (e: Exception) {
                                deviceState.connectionRequest.remove(device.id)
                                call.respondProtobuf(DeviceConnectResponse(REJECTED, ""))
                            }
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "连接过程中发生错误: ${e.message}")
                    }
                }
            }
        }
    }
}