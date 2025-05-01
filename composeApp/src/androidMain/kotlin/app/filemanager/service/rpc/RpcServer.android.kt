package app.filemanager.service.rpc

import app.filemanager.PlatformType
import app.filemanager.createSettings
import app.filemanager.service.data.ConnectType
import androidx.compose.runtime.snapshotFlow
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.RpcClientManager.Companion.PORT
import io.ktor.server.application.*
import io.ktor.server.cio.*
import app.filemanager.db.FileManagerDatabase
import app.filemanager.ui.state.file.FileShareStatus
import app.filemanager.ui.state.main.DeviceState
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.datetime.Clock
import org.koin.java.KoinJavaComponent.inject
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.protobuf.protobuf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface

@OptIn(ExperimentalSerializationApi::class)
actual suspend fun startRpcServer() {
    val settings = createSettings()

    embeddedServer(CIO, PORT) {
        install(SSE)
        install(Krpc)

        configureShareSse()

        routing {
            get("/ping") {
                val socketDevice = SocketDevice(
                    id = settings.getString("deviceId", ""),
                    name = settings.getString("deviceName", ""),
                    host = InetAddress.getLocalHost().hostAddress,
                    type = PlatformType,
                    connectType = ConnectType.UnConnect
                )

                call.respondBytes { ProtoBuf.encodeToByteArray(socketDevice) }
            }

            rpc {
                rpcConfig {
                    serialization {
                        protobuf()
                    }
                }

                registerService<DeviceService> { ctx -> DeviceServiceImpl(ctx) }
                registerService<BookmarkService> { ctx -> BookmarkServiceImpl(ctx) }
                registerService<FileService> { ctx -> FileServiceImpl(ctx) }
                registerService<PathService> { ctx -> PathServiceImpl(ctx) }

                registerService<ShareService> { ctx -> ShareServiceImpl(ctx) }
            }
        }
    }.start(wait = true)
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureShareSse() {
    val deviceState: DeviceState by inject(DeviceState::class.java)
    val database: FileManagerDatabase by inject(FileManagerDatabase::class.java)

    routing {
        sse("/share/{device}") {
            val deviceHex = call.parameters["device"] ?: ""
            if (deviceHex.isEmpty()) {
                send(event = FileShareStatus.ERROR.toString(), data = "")
                close()
                return@sse
            }

            try {
                val socketDevice = ProtoBuf.decodeFromHexString<SocketDevice>(deviceHex)

                if (deviceState.shares.find { it.id == socketDevice.id } != null) {
                    send(event = FileShareStatus.REJECTED.toString(), data = "请勿重复连接")
                    close()
                    return@sse
                }

                val deviceReceiveShare =
                    database.deviceReceiveShareQueries.selectById(socketDevice.id).executeAsOneOrNull()
                when (deviceReceiveShare?.connectionType ?: DeviceConnectType.WAITING) {
                    DeviceConnectType.AUTO_CONNECT -> {
                        send(event = FileShareStatus.COMPLETED.toString(), data = "")
                        deviceState.connectShare(socketDevice)
                        close()
                        return@sse
                    }
                    DeviceConnectType.PERMANENTLY_BANNED -> {
                        send(event = FileShareStatus.ERROR.toString(), data = "拒绝访问")
                        close()
                        return@sse
                    }
                    else -> {}
                }

                deviceState.shareRequest[socketDevice.id] = Pair(DeviceConnectType.WAITING, Clock.System.now().toEpochMilliseconds())
                send(event = FileShareStatus.WAITING.toString(), data = "等待对方同意")

                snapshotFlow { deviceState.shareConnectionStates.toMap() }
                    .collect { shares ->
                        if (shares.containsKey(socketDevice.id)) {
                            send(event = (shares[socketDevice.id] ?: FileShareStatus.REJECTED).toString(), data = "")
                            deviceState.shareConnectionStates.remove(socketDevice.id)
                            close()
                        }
                    }

            } catch (e: Exception) {
                send(event = FileShareStatus.ERROR.toString(), data = e.message)
                close()
            }
        }
    }
}

actual fun getAllIPAddresses(type: SocketClientIPEnum): List<String> {
    val addresses = mutableListOf<String>()

    val interfaces = NetworkInterface.getNetworkInterfaces()
    interfaces.iterator().forEach { networkInterface ->
        if (type == SocketClientIPEnum.IPV4_UP && (!networkInterface.isUp || networkInterface.isLoopback)) return@forEach
        // 获取该网络接口下所有的 InetAddress
        networkInterface.inetAddresses.iterator().forEach { inetAddress ->
            when (inetAddress) {
                is Inet4Address -> addresses.add(inetAddress.hostAddress)
                is Inet6Address -> {
                    if (type == SocketClientIPEnum.ALL) {
                        addresses.add("[${inetAddress.hostAddress.replace("%.*$".toRegex(), "")}]")
                    }
                }
            }
        }
    }

    if (type == SocketClientIPEnum.ALL) {
        addresses.addAll(listOf("[::1]", "[0:0:0:0:0:0:0:0]"))
    }

    return addresses
}