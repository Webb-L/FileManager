package app.filemanager.service

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceType
import app.filemanager.extensions.parsePath
import app.filemanager.extensions.replaceLast
import app.filemanager.readResourceFile
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
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
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
        server = embeddedServer(
            Netty,
            applicationEnvironment {
                log = LoggerFactory.getLogger("ktor.application")
            },
            {
                envConfig(port)
            },
            module = { configureFileSharing() },
        ).start(wait = false)
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

    private fun ApplicationEngine.Configuration.envConfig(port: Int) {
        connector {
            this.port = port
        }
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

    fun ApplicationCall.isApiRequest(): Boolean {
        return this.request.headers["X-API-Request"] != null
    }

    private fun Application.configureFileSharing() {
        install(FreeMarker) {
            templateLoader = ClassTemplateLoader(this::class.java.classLoader, "share-file")
        }

        install(Compression) {
            gzip {
                priority = 1.0
            }
            deflate {
                priority = 10.0
                minimumSize(1024)
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
                val params = call.request.queryParameters.entries()
                    .joinToString("&") { "${it.key}=${it.value.joinToString(",")}" }
                val path = "${
                    URLDecoder.decode(
                        call.request.path(),
                        "UTF-8"
                    )
                }${if (params.isNotEmpty()) "?$params" else ""}"
                val device = call.getClientDeviceInfo()
                val message = "${device?.name} - ${device?.id} [$status] $httpMethod $path"
                message
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
                            if (call.isApiRequest()) {
                                return@get call.respond(HttpStatusCode.Unauthorized, "请输入正确密码！")
                            }
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
                        if (call.isApiRequest()) {
                            return@get call.respond(HttpStatusCode.Unauthorized, "文件准备中")
                        }
                        return@get call.respond(FreeMarkerContent("waiting.ftl", emptyMap<String, Any>()))
                    }

                    call.respondByRequestType(
                        call.request.queryParameters["search"],
                        files
                            .filter { file ->
                                val searchMatch =
                                    file.name.contains(call.request.queryParameters["search"] ?: "")
                                val visibilityMatch =
                                    if (sharedFileInfo?.first == true) true else !file.isHidden
                                searchMatch && visibilityMatch
                            }
                            .map { file -> file.withCopy(path = "/${file.name}") }
                            .sortedWith(
                                compareByDescending<FileSimpleInfo> { it.isDirectory }
                                    .then(NaturalOrderComparator())
                            ),
                    )
                }

                get("/{...}") {
                    val clientRequest = call.getClientDeviceInfo()!!

                    val files =
                        fileShareState.authorizedLinkShareDevices[clientRequest] ?: return@get call.respondRedirect(
                            "/",
                            permanent = false
                        )

                    val urlPath = URLDecoder.decode(call.request.path(), "UTF-8")

                    val parentFileSimpleInfo = files.second.find { urlPath.indexOf("/${it.name}") == 0 }
                        ?: return@get call.respondRedirect("/", permanent = false)

                    val path = parentFileSimpleInfo.path.replaceLast("/${parentFileSimpleInfo.name}", urlPath)
                    val fileSimpleInfoResult = FileUtils.getFile(path).getOrNull()

                    // 如果文件信息结果为空，则重定向到根目录
                    if (fileSimpleInfoResult == null) {
                        return@get call.respondRedirect("/", permanent = false)
                    }

                    // 检查路径中的每个段落，判断是否包含隐藏文件
                    val pathSegments = urlPath.parsePath()
                    if (pathSegments.indices.any { index ->
                            val newPath = parentFileSimpleInfo.path.replaceLast(
                                "/${parentFileSimpleInfo.name}",
                                "/" + pathSegments.subList(0, index + 1).joinToString(PathUtils.getPathSeparator())
                            )
                            val fileSimpleInfo = FileUtils.getFile(newPath).getOrNull()
                            fileSimpleInfo != null && files.first == false && fileSimpleInfo.isHidden == true
                        }) {
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
                            val visibilityMatch = if (files.first == true) true else !file.isHidden
                            searchMatch && visibilityMatch
                        }
                        .map { file -> file.withCopy(path = "$urlPath/${file.name}") }
                        .sortedWith(
                            compareByDescending<FileSimpleInfo> { it.isDirectory }
                                .then(NaturalOrderComparator())
                        )

                    call.respondByRequestType(
                        call.request.queryParameters["search"],
                        fileSimpleInfos
                    )
                }
            }
        }
    }


    /**
     * 根据请求类型响应数据。
     * 如果请求是 API 请求，则返回包含文件信息的 JSON 数据；
     * 如果是普通请求，则返回渲染的 HTML 模板内容。
     *
     * @param search 搜索关键字，可为空，用于过滤文件信息。
     * @param files 文件信息列表，每个 `FileSimpleInfo` 包含文件相关的详细信息（如名称、大小、路径等）。
     */
    private suspend fun RoutingCall.respondByRequestType(
        search: String?,
        files: List<FileSimpleInfo>
    ) {
        if (isApiRequest()) {
            response.header("Content-Type", "application/json")
            respond(Json.encodeToString(files))
        } else {
            // Map<类型，Pair<扩展名，内容>>
            val scripts = mapOf<String, Pair<String, String>>(
                "Bash" to Pair("sh", readShellFile("share-file/shell/script.sh")),
                "PowerShell" to Pair("ps1", readShellFile("share-file/shell/script.ps1"))
            )

            respond(
                FreeMarkerContent(
                    "index.ftl",
                    mapOf(
                        "search" to search,
                        "files" to files,
                        "scripts" to scripts
                    )
                )
            )
        }
    }

    /**
     * 读取指定路径的 shell 文件内容，并动态替换其中的占位符。
     *
     * @param path 指定 shell 文件的路径。
     * @return 处理过的文件内容字符串，其中一些占位符会被动态替换，包括：
     * - `#API_SERVER#`: 替换为当前请求的完整服务地址。
     * - `#USER_AGENT#`: 替换为当前请求头中的 User-Agent 信息。
     * - `#ROOT_PATH#`: 替换为当前请求 URI（根路径若为 "/" 则为空）。
     * - `#TARGET_DIR#`: 替换为请求 URI 路径中最后一个路径片段。
     */
    private fun RoutingCall.readShellFile(path: String): String {
        val fullUrl = "${request.origin.scheme}://${request.host()}:${request.port()}"
        val paths = request.uri.parsePath()
        return readResourceFile(path)
            .decodeToString()
            .replace("#API_SERVER#", fullUrl)
            .replace("#USER_AGENT#", request.userAgent() ?: "")
            .replace("#ROOT_PATH#", if (request.uri == "/") "" else request.uri)
            .replace("#TARGET_DIR#", if (paths.isEmpty()) "" else paths.last())
    }
}