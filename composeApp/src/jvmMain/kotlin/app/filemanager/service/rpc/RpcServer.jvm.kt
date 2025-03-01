package app.filemanager.service.rpc

import app.filemanager.PlatformType
import app.filemanager.createSettings
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceType
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.RpcClientManager.Companion.PORT
import app.filemanager.ui.state.file.FileShareState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.NaturalOrderComparator
import app.filemanager.utils.PathUtils
import freemarker.cache.ClassTemplateLoader
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.freemarker.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.protobuf.protobuf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.java.KoinJavaComponent.inject
import org.slf4j.event.Level
import ua_parser.Parser
import java.io.File
import java.io.RandomAccessFile
import java.net.*

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