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

actual suspend fun startHttpShareFileServer() {
    val fileShareState = inject<FileShareState>(FileShareState::class.java)

    embeddedServer(Netty, 8080) {
        install(FreeMarker) {
            templateLoader = ClassTemplateLoader(this::class.java.classLoader, "share-file")
        }
        // 安装 Compression 插件（用于压缩响应）
        install(Compression) {
            gzip {
                priority = 1.0
            }
        }

        install(CallLogging) {
            level = Level.INFO
            filter { call ->
                call.request.path().contains("/static").not()
            }
            format { call ->
                val status = call.response.status()
                val httpMethod = call.request.httpMethod.value
                val path = call.request.path()
                val params = call.request.queryParameters.entries()
                    .joinToString("&") { "${it.key}=${it.value.joinToString(",")}" }
                val device = call.getClientDeviceInfo()
                "${device?.name} - ${device?.id} [$status] $httpMethod $path${if (params.isNotEmpty()) "?$params" else ""}"
            }
        }

        routing {
            staticResources("/static", "share-file/static")

            route("/") {
                install(createRouteScopedPlugin("DeviceCheck") {
                    onCall { call ->
                        val device = call.getClientDeviceInfo()
                        if (device == null || fileShareState.value.rejectedLinkShareDevices.contains(device)) {
                            call.respond(HttpStatusCode.Forbidden)
                            return@onCall
                        }
                    }
                })

                get("/") {
                    val clientRequest = call.getClientDeviceInfo()!!

                    if (
                        !fileShareState.value.pendingLinkShareDevices.contains(clientRequest) &&
                        !fileShareState.value.authorizedLinkShareDevices.containsKey(clientRequest)
                    ) {
                        fileShareState.value.pendingLinkShareDevices.add(clientRequest)
                    }

                    val sharedFileInfo = fileShareState.value.authorizedLinkShareDevices[clientRequest]
                    val files = sharedFileInfo?.second ?: listOf()
                    if (files.isEmpty()) {
                        return@get call.respond(FreeMarkerContent("waiting.ftl", emptyMap<String, Any>()))
                    }

                    call.respond(
                        FreeMarkerContent(
                            "index.ftl",
                            mapOf(
                                "search" to call.request.queryParameters["search"],
                                "files" to files
                                    .filter { file ->
                                        val searchMatch =
                                            file.name.contains(call.request.queryParameters["search"] ?: "")
                                        val visibilityMatch =
                                            if (sharedFileInfo?.first == false) true else !file.isHidden
                                        searchMatch && visibilityMatch
                                    }
                                    .sortedWith(
                                        compareByDescending<FileSimpleInfo> { it.isDirectory }
                                            .then(NaturalOrderComparator())
                                    ),
                            )
                        )
                    )
                }

                get("/{...}") {
                    val clientRequest = call.getClientDeviceInfo()!!

                    val files =
                        fileShareState.value.authorizedLinkShareDevices[clientRequest] ?: return@get call.respond(
                            HttpStatusCode.Forbidden
                        )

                    val path = URLDecoder.decode(call.request.path(), "UTF-8")
                    val fileSimpleInfoResult = FileUtils.getFile(path).getOrNull()

                    if (fileSimpleInfoResult == null || files.second.find {
                            it.path == path || path.contains(it.path)
                        } == null) {
                        return@get call.respond(HttpStatusCode.NotFound)
                    }

                    // 下载文件
                    if (!fileSimpleInfoResult.isDirectory) {
                        val file = File(path)
                        if (!file.exists()) {
                            return@get call.respond(HttpStatusCode.NotFound)
                        }

                        call.response.header(HttpHeaders.ContentLength, file.length().toString())
                        val rangeHeader = call.request.header(HttpHeaders.Range)
                        if (rangeHeader == null) {
                            return@get call.respondFile(file)
                        }
                        // 解析 Range 请求头
                        val range = rangeHeader.removePrefix("bytes=").split("-")
                        val start = range[0].toLongOrNull() ?: 0
                        val end = range[1].toLongOrNull() ?: (file.length() - 1)

                        // 读取指定范围的数据
                        val length = end - start + 1

                        call.response.header(HttpHeaders.AcceptRanges, "bytes")
                        call.response.header(
                            HttpHeaders.ContentRange,
                            "bytes $start-$end/${file.length()}"
                        )
                        call.response.header(HttpHeaders.ContentLength, length.toString())

                        call.response.status(HttpStatusCode.PartialContent)

                        // 使用 RandomAccessFile 读取指定范围的数据
                        RandomAccessFile(file, "r").use { randomAccessFile ->
                            call.respondOutputStream(contentType = ContentType.Application.OctetStream) {
                                randomAccessFile.seek(start)
                                val buffer = ByteArray(8192)
                                var bytesRemaining = length
                                while (bytesRemaining > 0) {
                                    val bytesToRead = minOf(buffer.size.toLong(), bytesRemaining).toInt()
                                    val bytesRead = randomAccessFile.read(buffer, 0, bytesToRead)
                                    if (bytesRead == -1) break
                                    write(buffer, 0, bytesRead)
                                    bytesRemaining -= bytesRead
                                }
                            }
                        }
                        return@get
                    }

                    val fileSimpleInfos = PathUtils.getFileAndFolder(path).getOrDefault(listOf())
                        .filter { file ->
                            val searchMatch = file.name.contains(call.request.queryParameters["search"] ?: "")
                            val visibilityMatch = if (files.first == false) true else !file.isHidden
                            searchMatch && visibilityMatch
                        }
                        .sortedWith(
                            compareByDescending<FileSimpleInfo> { it.isDirectory }
                                .then(NaturalOrderComparator())
                        )
                    call.respond(
                        FreeMarkerContent(
                            "index.ftl",
                            mapOf(
                                "search" to call.request.queryParameters["search"],
                                "files" to fileSimpleInfos
                            )
                        )
                    )
                }
            }
        }
    }.start(wait = true)
}

private fun ApplicationCall.getClientDeviceInfo(): Device? {
    // 获取客户端 IP
    val xForwardedFor = request.headers["X-Forwarded-For"]
    val remoteHost = request.origin.remoteHost
    val clientIp = xForwardedFor?.split(",")?.firstOrNull()?.trim() ?: remoteHost

    // 获取并解析 User-Agent
    val userAgent = request.userAgent() ?: return null

    // 解析客户端信息
    val client = Parser().parse(userAgent)
    val browser = client.userAgent.family
    val os = client.os.family
    val device = client.device.family

    // 创建 Device 对象
    return Device(
        id = clientIp,
        name = "$browser-$os",
        host = mutableMapOf(),
        type = DeviceType.JS,
        token = ""
    )
}