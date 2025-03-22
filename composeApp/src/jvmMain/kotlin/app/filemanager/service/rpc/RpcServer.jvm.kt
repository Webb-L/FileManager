package app.filemanager.service.rpc

import app.filemanager.PlatformType
import app.filemanager.createSettings
import app.filemanager.data.main.DeviceConnectType.WAITING
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.RpcClientManager.Companion.PORT
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.utils.PathUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.protobuf.protobuf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.java.KoinJavaComponent.inject
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface

@OptIn(ExperimentalSerializationApi::class)
actual suspend fun startRpcServer() {
    val settings = createSettings()

    println(PathUtils.getAppPath())

    println(System.getProperty("java.io.tmpdir"))

    embeddedServer(Netty, PORT) {
        install(Krpc)

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

            // 对方想发送文件给你
            post("/share") {
                try {
                    // 接收请求体并反序列化为 SocketDevice 对象
                    val socketDevice = ProtoBuf.decodeFromByteArray<SocketDevice>(call.receive<ByteArray>())

                    val deviceState: DeviceState by inject(DeviceState::class.java)

                    // 已经连接了，禁止再次连接。
                    if (deviceState.shares.find { it.id == socketDevice.id } != null) {
                        call.respond(HttpStatusCode.Conflict)
                        return@post
                    }

                    deviceState.shareRequest[socketDevice.id] = Pair(WAITING, Clock.System.now().toEpochMilliseconds())
                    // 返回成功响应
                    call.respond(HttpStatusCode.OK)
                } catch (e: ContentTransformationException) {
                    // 捕获反序列化失败异常（例如请求体格式错误）
                    call.respond(HttpStatusCode.BadRequest, "Invalid request body: ${e.message}")
                } catch (e: Exception) {
                    // 捕获其他异常
                    call.respond(HttpStatusCode.InternalServerError, "An error occurred: ${e.message}")
                }
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