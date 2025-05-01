package app.filemanager.service

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceType
import app.filemanager.extensions.parsePath
import app.filemanager.extensions.replaceLast
import app.filemanager.ui.state.file.FileShareState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.NaturalOrderComparator
import app.filemanager.utils.PathUtils
import filemanager.composeapp.generated.resources.Res
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * HttpShareFileServer的通用功能抽象类，包含Android和JVM平台共享的代码
 */
abstract class HttpShareFileServerCommon(protected val fileShareState: FileShareState) : HttpShareFileServerInterface {
    protected var server: EmbeddedServer<*, *>? = null

    override suspend fun stop() {
        try {
            server?.stop(500, 1000)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            server = null
        }
    }

    override fun isRunning(): Boolean {
        return server != null
    }


    /**
     * 从ApplicationCall中获取客户端设备信息
     */
    fun ApplicationCall.getClientDeviceInfo(): Device? {
        // 获取客户端 IP
        val xForwardedFor = request.headers["X-Forwarded-For"]
        val remoteHost = request.origin.remoteHost
        val clientIp = xForwardedFor?.split(",")?.firstOrNull()?.trim() ?: remoteHost

        // 获取User-Agent
        val userAgent = request.headers["User-Agent"] ?: return null

        // 解析浏览器类型
        val browser = parseBrowser(userAgent)

        // 解析操作系统
        val os = parseOperatingSystem(userAgent)

        // 解析设备类型
        val deviceInfo = parseDevice(userAgent)

        // 确定设备类型
        val deviceType = when {
            userAgent.contains("Android", ignoreCase = true) -> DeviceType.Android
            userAgent.contains("iPhone", ignoreCase = true) || userAgent.contains(
                "iPad",
                ignoreCase = true
            ) -> DeviceType.IOS

            else -> DeviceType.JS
        }

        // 创建 Device 对象
        return Device(
            id = clientIp,
            name = "$browser-$os",
            host = mutableMapOf(),
            type = deviceType,
            token = ""
        )
    }

    /**
     * 解析浏览器信息
     */
    private fun parseBrowser(userAgent: String): String {
        return when {
            // 检测主流浏览器
            userAgent.contains("Firefox/", ignoreCase = true) -> {
                val regex = "Firefox/([\\d.]+)".toRegex()
                val match = regex.find(userAgent)
                "Firefox ${match?.groupValues?.getOrNull(1) ?: ""}"
            }

            userAgent.contains("OPR/", ignoreCase = true) || userAgent.contains("Opera/", ignoreCase = true) -> {
                val regex = "OPR/([\\d.]+)|Opera/([\\d.]+)".toRegex()
                val match = regex.find(userAgent)
                "Opera ${match?.groupValues?.firstOrNull { it.isNotEmpty() && it.contains(".") } ?: ""}"
            }

            userAgent.contains("Edg/", ignoreCase = true) -> {
                val regex = "Edg/([\\d.]+)".toRegex()
                val match = regex.find(userAgent)
                "Edge ${match?.groupValues?.getOrNull(1) ?: ""}"
            }

            userAgent.contains("Chrome/", ignoreCase = true) -> {
                val regex = "Chrome/([\\d.]+)".toRegex()
                val match = regex.find(userAgent)
                "Chrome ${match?.groupValues?.getOrNull(1) ?: ""}"
            }

            userAgent.contains("Safari/", ignoreCase = true) && !userAgent.contains("Chrome", ignoreCase = true) -> {
                val regex = "Version/([\\d.]+)".toRegex()
                val match = regex.find(userAgent)
                "Safari ${match?.groupValues?.getOrNull(1) ?: ""}"
            }

            userAgent.contains("MSIE", ignoreCase = true) || userAgent.contains("Trident/", ignoreCase = true) -> {
                if (userAgent.contains("MSIE", ignoreCase = true)) {
                    val regex = "MSIE ([\\d.]+)".toRegex()
                    val match = regex.find(userAgent)
                    "Internet Explorer ${match?.groupValues?.getOrNull(1) ?: ""}"
                } else {
                    "Internet Explorer 11.0"
                }
            }

            else -> "Unknown Browser"
        }
    }

    /**
     * 解析操作系统信息
     */
    private fun parseOperatingSystem(userAgent: String): String {
        return when {
            // 移动设备操作系统
            userAgent.contains("Android", ignoreCase = true) -> {
                val regex = "Android ([\\d.]+)".toRegex()
                val match = regex.find(userAgent)
                "Android ${match?.groupValues?.getOrNull(1) ?: ""}"
            }

            userAgent.contains("iPhone", ignoreCase = true) -> {
                val regex = "iPhone OS ([\\d_]+)".toRegex()
                val match = regex.find(userAgent)
                "iOS ${match?.groupValues?.getOrNull(1)?.replace("_", ".") ?: ""}"
            }

            userAgent.contains("iPad", ignoreCase = true) -> {
                val regex = "CPU OS ([\\d_]+)".toRegex()
                val match = regex.find(userAgent)
                "iPadOS ${match?.groupValues?.getOrNull(1)?.replace("_", ".") ?: ""}"
            }

            // 桌面操作系统
            userAgent.contains("Windows NT", ignoreCase = true) -> {
                val regex = "Windows NT ([\\d.]+)".toRegex()
                val match = regex.find(userAgent)
                val version = match?.groupValues?.getOrNull(1)
                when (version) {
                    "10.0" -> "Windows 10"
                    "6.3" -> "Windows 8.1"
                    "6.2" -> "Windows 8"
                    "6.1" -> "Windows 7"
                    "6.0" -> "Windows Vista"
                    "5.2" -> "Windows XP x64"
                    "5.1" -> "Windows XP"
                    else -> "Windows ${version ?: ""}"
                }
            }

            userAgent.contains("Mac OS X", ignoreCase = true) -> {
                val regex = "Mac OS X ([\\d_.]+)".toRegex()
                val match = regex.find(userAgent)
                "macOS ${match?.groupValues?.getOrNull(1)?.replace("_", ".") ?: ""}"
            }

            userAgent.contains("Linux", ignoreCase = true) -> {
                if (userAgent.contains("Ubuntu", ignoreCase = true)) {
                    "Ubuntu Linux"
                } else {
                    "Linux"
                }
            }

            else -> "Unknown OS"
        }
    }

    /**
     * 解析设备信息
     */
    private fun parseDevice(userAgent: String): String {
        return when {
            // 移动设备
            userAgent.contains("iPhone", ignoreCase = true) -> "iPhone"
            userAgent.contains("iPad", ignoreCase = true) -> "iPad"
            userAgent.contains("Android", ignoreCase = true) -> {
                if (userAgent.contains("Mobile", ignoreCase = true)) {
                    "Android Mobile"
                } else {
                    "Android Tablet"
                }
            }

            // 桌面设备
            userAgent.contains("Windows", ignoreCase = true) -> "PC"
            userAgent.contains("Macintosh", ignoreCase = true) -> "Mac"
            userAgent.contains("Linux", ignoreCase = true) -> "Linux PC"

            else -> "Unknown Device"
        }
    }


    fun ApplicationCall.isApiRequest(): Boolean {
        return this.request.headers["X-API-Request"] != null
    }

    @OptIn(ExperimentalResourceApi::class)
    fun Application.configureRoutes() {
        routing {
            get("/static/{staticPath...}") {
                val staticPath = call.parameters.getAll("staticPath")?.joinToString("/") ?: ""
                try {
                    val fileBytes = Res.readBytes("files/share-file/static/$staticPath")
                    val contentType = when {
                        staticPath.endsWith(".css") -> ContentType.Text.CSS
                        staticPath.endsWith(".js") -> ContentType.Text.JavaScript
                        staticPath.endsWith(".png") -> ContentType.Image.PNG
                        staticPath.endsWith(".jpg") || staticPath.endsWith(".jpeg") -> ContentType.Image.JPEG
                        staticPath.endsWith(".svg") -> ContentType.Image.SVG
                        staticPath.endsWith(".woff2") -> ContentType.parse("font/woff2")
                        staticPath.endsWith(".woff") -> ContentType.parse("font/woff")
                        staticPath.endsWith(".ttf") -> ContentType.parse("font/ttf")
                        staticPath.endsWith(".eot") -> ContentType.parse("application/vnd.ms-fontobject")
                        else -> ContentType.Application.OctetStream
                    }

                    call.respondBytes(fileBytes, contentType)
                } catch (e: Exception) {
                    println(e)
                    call.respond(HttpStatusCode.NotFound)
                }
            }

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
                            return@get call.respondHtml {
                                app.filemanager.service.HtmlTemplates.passwordPage()(this)
                            }
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
                        return@get call.respondHtml {
                            app.filemanager.service.HtmlTemplates.waitingPage()(this)
                        }
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
                            .map { file -> file.withCopy(path = "/${file.name}".urlPath()) }
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

                    val urlPath = call.request.path().decodeURLPart()

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
                        val fileInfo = FileUtils.getFile(path)
                        if (fileInfo.isFailure) {
                            return@get call.respondRedirect("/", permanent = false)
                        }

                        val file = fileInfo.getOrThrow()

                        call.response.header(HttpHeaders.ContentLength, file.size.toString())
                        val rangeHeader = call.request.header(HttpHeaders.Range)
                        if (rangeHeader == null) {
                            val readFile = FileUtils.readFile(file.path)
                            if (readFile.isFailure) {
                                return@get call.respondRedirect("/", permanent = false)
                            }

                            return@get call.respondBytes(
                                readFile.getOrDefault(byteArrayOf()),
                                ContentType.Application.OctetStream
                            )
                        }
                        // 解析 Range 请求头
                        val range = rangeHeader.removePrefix("bytes=").split("-")
                        val start = range[0].toLongOrNull() ?: 0
                        val end = range[1].toLongOrNull() ?: (file.size - 1)

                        // 读取指定范围的数据
                        val length = end - start + 1

                        call.response.header(HttpHeaders.AcceptRanges, "bytes")
                        call.response.header(
                            HttpHeaders.ContentRange,
                            "bytes $start-$end/${file.size}"
                        )
                        call.response.header(HttpHeaders.ContentLength, length.toString())

                        call.response.status(HttpStatusCode.PartialContent)

                        val readFileRange = FileUtils.readFileRange(file.path, start, 8192)
                        if (readFileRange.isFailure) {
                            return@get call.respond(HttpStatusCode.InternalServerError, "读取文件失败！")
                        }

                        return@get call.respondBytes(
                            readFileRange.getOrDefault(byteArrayOf()),
                            ContentType.Application.OctetStream
                        )
                    }

                    val fileSimpleInfos = PathUtils.getFileAndFolder(path).getOrDefault(listOf())
                        .filter { file ->
                            val searchMatch = file.name.contains(call.request.queryParameters["search"] ?: "")
                            val visibilityMatch = if (files.first == true) true else !file.isHidden
                            searchMatch && visibilityMatch
                        }
                        .map { file -> file.withCopy(path = "$urlPath/${file.name}".urlPath()) }
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
     */
    protected suspend fun RoutingCall.respondByRequestType(
        search: String?,
        files: List<FileSimpleInfo>
    ) {
        if (isApiRequest()) {
            response.header("Content-Type", "application/json")
            respond(Json.encodeToString(files))
        } else {
            // Map<类型，Pair<扩展名，内容>>
            val scripts = mapOf(
                "Bash" to Pair("sh", readShellFile("files/share-file/shell/script.sh")),
                "PowerShell" to Pair("ps1", readShellFile("files/share-file/shell/script.ps1"))
            )

            respondHtml {
                HtmlTemplates.indexPage(
                    search = search,
                    files = files,
                    scripts = scripts
                )(this)
            }
        }
    }

    /**
     * 读取指定路径的 shell 文件内容，并动态替换其中的占位符。
     */
    @OptIn(ExperimentalResourceApi::class)
    protected suspend fun RoutingCall.readShellFile(path: String): String {
        val fullUrl = "${request.origin.scheme}://${request.host()}:${request.port()}"
        val paths = request.uri.parsePath()
        return Res.readBytes(path)
            .decodeToString()
            .replace("#API_SERVER#", fullUrl)
            .replace("#USER_AGENT#", request.userAgent() ?: "")
            .replace("#ROOT_PATH#", if (request.path() == "/") "" else request.path())
            .replace("#TARGET_DIR#", if (paths.isEmpty()) "" else paths.last().decodeURLQueryComponent())
    }


    /**
     * 实现类似FreeMarker的url_path函数功能
     * 对字符串进行URL路径编码，但保留路径分隔符('/')
     *
     * 使用Ktor HTTP库的encodeURLPathPart函数替代传统的URLEncoder
     */
    fun String.urlPath(): String {
        // 如果字符串为空，直接返回
        if (this.isEmpty()) return this

        // 按照'/'分割路径
        val segments = this.split('/')

        // 对每个段进行URL编码，但保留'/'作为分隔符
        val encodedSegments = segments.map { segment ->
            if (segment.isEmpty()) {
                // 空段保持为空（连续的/会产生空段）
                segment
            } else {
                // 使用Ktor的encodeURLPathPart替代URLEncoder
                segment.encodeURLPathPart()
            }
        }

        // 重新组合路径
        return encodedSegments.joinToString("/")
    }
} 