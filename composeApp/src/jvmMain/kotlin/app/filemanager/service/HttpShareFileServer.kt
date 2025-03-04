package app.filemanager.service

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceType
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.event.Level
import ua_parser.Parser
import java.io.File
import java.io.RandomAccessFile
import java.net.URLDecoder

actual class HttpShareFileServer actual constructor(private val fileShareState: FileShareState) :
    HttpShareFileServerInterface {
    private var server: EmbeddedServer<*, *>? = null

    actual companion object {
        @Volatile
        private var instance: HttpShareFileServer? = null

        actual fun getInstance(fileShareState: FileShareState): HttpShareFileServer {
            return instance ?: synchronized(this) {
                instance ?: HttpShareFileServer(fileShareState).also { instance = it }
            }
        }
    }


    actual override fun start(port: Int) {
        server = embeddedServer(Netty, port) {
            install(FreeMarker) {
                templateLoader = ClassTemplateLoader(this::class.java.classLoader, "share-file")
            }

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
                    "${device?.name} - ${device?.id} [$status] $httpMethod ${URLDecoder.decode(call.request.path(), "UTF-8")}${if (params.isNotEmpty()) "?$params" else ""}"
                }
            }

            routing {
                staticResources("/static", "share-file/static")

                route("/") {
                    install(createRouteScopedPlugin("DeviceCheck") {
                        onCall { call ->
                            val device = call.getClientDeviceInfo()
                            if (device == null || fileShareState.rejectedLinkShareDevices.contains(device)) {
                                call.respond(HttpStatusCode.Forbidden)
                                return@onCall
                            }
                        }
                    })

                    get("/") {
                        val clientRequest = call.getClientDeviceInfo()!!

                        if (
                            !fileShareState.pendingLinkShareDevices.contains(clientRequest) &&
                            !fileShareState.authorizedLinkShareDevices.containsKey(clientRequest)
                        ) {
                            fileShareState.pendingLinkShareDevices.add(clientRequest)
                        }

                        if (
                            fileShareState.connectPassword.value.isNotEmpty() &&
                            !fileShareState.authorizedLinkShareDevices.containsKey(clientRequest)
                        ) {
                            if (call.request.queryParameters["pwd"] != fileShareState.connectPassword.value) {
                                return@get call.respond(FreeMarkerContent("password.ftl", emptyMap<String, Any>()))
                            } else {
                                fileShareState.pendingLinkShareDevices.remove(clientRequest)
                                fileShareState.authorizedLinkShareDevices[clientRequest] =
                                    Pair(true, fileShareState.files.toList())
                            }
                        }

                        val sharedFileInfo = fileShareState.authorizedLinkShareDevices[clientRequest]
                        val files = sharedFileInfo?.second ?: listOf()
                        if (files.isEmpty() && fileShareState.connectPassword.value.isEmpty()) {
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
                            fileShareState.authorizedLinkShareDevices[clientRequest] ?: return@get call.respondRedirect(
                                "/",
                                permanent = false
                            )

                        val path = URLDecoder.decode(call.request.path(), "UTF-8")
                        val fileSimpleInfoResult = FileUtils.getFile(path).getOrNull()

                        // 如果文件信息结果为空，或文件未在授权的文件列表中找到，或文件的隐藏状态与文件权限不匹配，则重定向到根目录
                        if (fileSimpleInfoResult == null || files.second.find {
                                it.path == path || path.contains(it.path)
                            } == null || fileSimpleInfoResult.isHidden == files.first) {
                            return@get call.respondRedirect("/", permanent = false)
                        }

                        // 下载文件
                        if (!fileSimpleInfoResult.isDirectory) {
                            val file = File(path)
                            if (!file.exists()) {
                                return@get call.respondRedirect("/", permanent = false)
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
        }.start(wait = false)
    }

    actual override suspend fun stop() {
        withContext(Dispatchers.IO) {
            try {
                server?.stop(500, 1000)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                server = null
            }
        }
    }

    actual override fun isRunning(): Boolean {
        return server != null
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
}