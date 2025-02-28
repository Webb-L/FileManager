package app.filemanager.service.rpc

import app.filemanager.PlatformType
import app.filemanager.createSettings
import app.filemanager.readResourceFile
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.RpcClientManager.Companion.PORT
import app.filemanager.ui.state.file.FileShareState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import freemarker.cache.ClassTemplateLoader
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.freemarker.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.partialcontent.*
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
import ua_parser.Client
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

    embeddedServer(Netty, 12040) {
        install(FreeMarker) {
            templateLoader = ClassTemplateLoader(this::class.java.classLoader, "share-file")
        }
        // 安装 Compression 插件（用于压缩响应）
        install(Compression) {
            gzip {
                priority = 1.0
            }
        }

        // 安装 PartialContent 插件（用于支持分段下载）
        install(PartialContent)

        routing {
            get("/static/all.min.css") {
                call.respondBytes { readResourceFile("share-file/static/all.min.css") }
            }
            get("/static/index.js") {
                call.respondBytes { readResourceFile("share-file/static/index.js") }
            }
            get("/static/google.font.css") {
                call.respondBytes { readResourceFile("share-file/static/google.font.css") }
            }
            get("/static/tailwindcss.js") {
                call.respondBytes { readResourceFile("share-file/static/tailwindcss.js") }
            }
            get("/webfonts/fa-solid-900.ttf") {
                call.respondBytes { readResourceFile("share-file/static/webfonts/fa-solid-900.ttf") }
            }
            get("/webfonts/fa-solid-900.woff2") {
                call.respondBytes { readResourceFile("share-file/static/webfonts/fa-solid-900.woff2") }
            }

            get("/") {
                if (fileShareState.value.files.isEmpty()) {
                    return@get call.respond(FreeMarkerContent("waiting.ftl", emptyMap<String, Any>()))
                }
                call.respond(
                    FreeMarkerContent(
                        "index.ftl",
                        mapOf("files" to fileShareState.value.files.toList())
                    )
                )
            }

            get("/{...}") {
                val path = URLDecoder.decode(call.request.path(), "UTF-8")
                val fileSimpleInfoResult = FileUtils.getFile(path).getOrNull()

                if (fileSimpleInfoResult == null || fileShareState.value.files.find { it.path == path|| path.contains(it.path) } == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                // 下载文件
                if (!fileSimpleInfoResult.isDirectory) {
                    val file = File(path)
                    if (!file.exists()) {
                        println("File not found: ${file.absolutePath}")
                        call.respond(HttpStatusCode.NotFound)
                        return@get
                    }

                    call.response.header(HttpHeaders.ContentLength, file.length().toString())
                    val rangeHeader = call.request.header(HttpHeaders.Range)
                    if (rangeHeader == null) {
                        call.respondFile(file)
                        return@get
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

                val xForwardedFor = call.request.headers["X-Forwarded-For"]
                val remoteHost = call.request.origin.remoteHost
                val remoteAddress = call.request.origin.remoteAddress

                // 优先使用 X-Forwarded-For 中的第一个 IP 地址
                val clientIp = xForwardedFor?.split(",")?.firstOrNull()?.trim() ?: remoteHost

                println("Your IP address is: $clientIp (Remote Address: $remoteAddress)")

                val userAgent = call.request.userAgent()
                if (userAgent != null) {
                    val client: Client = Parser().parse(userAgent)

                    val browser = client.userAgent.family
                    val os = client.os.family
                    val device = client.device.family

//                    call.respondText(
//                        """
//                    """.trimIndent(),
//                        ContentType.Text.Html
//                    )


                    call.respond(
                        FreeMarkerContent(
                            "index.ftl",
                            mapOf("files" to PathUtils.getFileAndFolder(path).getOrDefault(listOf()))
                        )
                    )
                } else {
                    call.respondText("User-Agent header is missing")
                }
            }

        }
    }.start(wait = true)
}