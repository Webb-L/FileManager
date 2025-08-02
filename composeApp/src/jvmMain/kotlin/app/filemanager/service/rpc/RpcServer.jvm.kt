package app.filemanager.service.rpc

import androidx.compose.runtime.snapshotFlow
import app.filemanager.PlatformType
import app.filemanager.createSettings
import app.filemanager.data.main.DeviceConnectType.*
import app.filemanager.db.FileManagerDatabase
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.RpcClientManager.Companion.PORT
import app.filemanager.ui.state.file.FileShareStatus
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.utils.PathUtils
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.datetime.Clock
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.protobuf.protobuf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.java.KoinJavaComponent.inject
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import kotlin.collections.set

@OptIn(ExperimentalSerializationApi::class)
actual suspend fun startRpcServer() {
    val settings = createSettings()

    println(PathUtils.getAppPath())

    println(System.getProperty("java.io.tmpdir"))

    embeddedServer(Netty, PORT) {
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

                registerService<DeviceService> { DeviceServiceImpl() }
                registerService<BookmarkService> { BookmarkServiceImpl() }
                registerService<FileService> { FileServiceImpl() }
                registerService<PathService> { PathServiceImpl() }

                registerService<ShareService> { ShareServiceImpl() }
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
                // 接收请求体并反序列化为 SocketDevice 对象
                val socketDevice = ProtoBuf.decodeFromHexString<SocketDevice>(deviceHex)

                // 已经连接了，禁止再次连接。
                if (deviceState.shares.find { it.id == socketDevice.id } != null) {
                    send(event = FileShareStatus.REJECTED.toString(), data = "请勿重复连接")
                    close()
                    return@sse
                }

                val deviceReceiveShare =
                    database.deviceReceiveShareQueries.selectById(socketDevice.id).executeAsOneOrNull()
                when (deviceReceiveShare?.connectionType ?: WAITING) {
                    AUTO_CONNECT -> {
                        send(event = FileShareStatus.COMPLETED.toString(), data = "")
                        deviceState.connectShare(socketDevice)
                        close()
                        return@sse
                    }
                    PERMANENTLY_BANNED -> {
                        send(event = FileShareStatus.ERROR.toString(), data = "拒绝访问")
                        close()
                        return@sse
                    }
                    else -> {}
                }
                println(deviceReceiveShare)

                deviceState.shareRequest[socketDevice.id] = Pair(WAITING, System.currentTimeMillis())
                // 返回成功响应
                send(event = FileShareStatus.WAITING.toString(), data = "等待对方同意")

                snapshotFlow { deviceState.shareConnectionStates.toMap() }
                    .collect { shares ->
                        println(shares)
                        if (shares.containsKey(socketDevice.id)) {
                            send(event = (shares[socketDevice.id] ?: FileShareStatus.REJECTED).toString(), data = "")
                            deviceState.shareConnectionStates.remove(socketDevice.id)
                            close()
                        }
                    }

            } catch (e: Exception) {
                // 捕获其他异常
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