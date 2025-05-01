package app.filemanager.service

import app.filemanager.ui.state.file.FileShareState
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.request.header
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File

/**
 * HttpShareFileServer的Android平台实现类
 *
 * 用于在Android设备上创建HTTP文件共享服务器，允许其他设备通过HTTP协议访问共享文件
 * 此实现使用Ktor的CIO引擎作为HTTP服务器
 */
actual class HttpShareFileServer actual constructor(fileShareState: FileShareState) :
    HttpShareFileServerCommon(fileShareState) {

    /**
     * 单例实现，确保整个应用中只有一个HTTP共享服务器实例
     */
    actual companion object {
        /**
         * 使用volatile确保多线程环境下的可见性
         */
        @Volatile
        private var instance: HttpShareFileServer? = null

        /**
         * 获取HttpShareFileServer实例
         * 使用双重检查锁定模式实现单例
         *
         * @param fileShareState 文件共享状态对象
         * @return HttpShareFileServer实例
         */
        actual fun getInstance(fileShareState: FileShareState): HttpShareFileServer {
            return instance ?: synchronized(this) {
                instance ?: HttpShareFileServer(fileShareState).also { instance = it }
            }
        }
    }

    /**
     * 启动HTTP服务器
     *
     * @param port 服务器监听端口，默认为12040
     */
    actual override fun start(port: Int) {
        server = embeddedServer(
            CIO, // 使用CIO引擎，适合Android平台
            applicationEnvironment {
                log = LoggerFactory.getLogger("ktor.application")
            },
            {
                envConfig(port)
            },
            module = { configureFileSharing() },
        ).start(wait = false) // 非阻塞启动
    }

    /**
     * 停止HTTP服务器
     *
     * 在协程中执行，确保不阻塞主线程
     */
    actual override suspend fun stop() {
        withContext(Dispatchers.IO) {
            try {
                server?.stop(500, 1000) // 优雅关闭，等待500ms，最长1000ms
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                server = null
            }
        }
    }

    /**
     * 检查服务器是否正在运行
     *
     * @return 如果服务器正在运行返回true，否则返回false
     */
    actual override fun isRunning(): Boolean {
        return server != null
    }

    /**
     * 处理文件下载请求，根据文件路径提供文件内容。
     * 支持范围请求以实现分块文件下载。
     *
     * @param filePath 要下载的文件的路径
     */
    override suspend fun ApplicationCall.downloadFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            respond(HttpStatusCode.NotFound,"$filePath 不存在")
            return
        }
        val fileSize = file.length()

        // 添加文件名
        response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(
                ContentDisposition.Parameters.FileName, file.name
            ).toString()
        )

        // 添加缓存控制，避免不必要的缓存
        response.header(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")

        // 添加接受范围请求的头信息，允许多线程下载
        response.header(HttpHeaders.AcceptRanges, "bytes")

        // 检查请求是否包含Range头
        val rangeHeader = request.header(HttpHeaders.Range)

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            // 解析范围请求
            val rangeValue = rangeHeader.substring(6)
            val rangeParts = rangeValue.split("-")

            if (rangeParts.size == 2) {
                try {
                    val start = rangeParts[0].toLong()
                    // 如果没有指定结束位置，则默认到文件末尾
                    val end = if (rangeParts[1].isNotEmpty()) rangeParts[1].toLong() else fileSize - 1

                    if (start < 0 || end >= fileSize || start > end) {
                        // 范围无效，返回完整文件
                        response.header(HttpHeaders.ContentLength, fileSize.toString())
                        respondOutputStream(contentType = ContentType.Application.OctetStream) {
                            file.inputStream().use { it.copyTo(this) }
                        }
                        return@downloadFile
                    }

                    // 计算此范围的大小
                    val rangeSize = end - start + 1

                    // 返回部分内容状态码
                    response.status(HttpStatusCode.PartialContent)

                    // 设置Content-Range头
                    response.header(HttpHeaders.ContentRange, "bytes $start-$end/$fileSize")

                    // 设置Content-Length为请求范围的大小
                    response.header(HttpHeaders.ContentLength, rangeSize.toString())

                    // 发送部分内容
                    respondOutputStream(contentType = ContentType.Application.OctetStream) {
                        file.inputStream().use { input ->
                            // 跳过到开始位置
                            input.skip(start)

                            // 指定复制的大小
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var bytesRemaining = rangeSize

                            while (bytesRemaining > 0) {
                                val bytesToRead = minOf(buffer.size.toLong(), bytesRemaining).toInt()
                                val bytesRead = input.read(buffer, 0, bytesToRead)

                                if (bytesRead <= 0) break

                                write(buffer, 0, bytesRead)
                                bytesRemaining -= bytesRead
                            }
                        }
                    }
                } catch (e: NumberFormatException) {
                    // 范围格式无效，发送完整文件
                    response.header(HttpHeaders.ContentLength, fileSize.toString())
                    respondOutputStream(contentType = ContentType.Application.OctetStream) {
                        file.inputStream().use { it.copyTo(this) }
                    }
                }
            } else {
                // 不支持的范围格式，发送完整文件
                response.header(HttpHeaders.ContentLength, fileSize.toString())
                respondOutputStream(contentType = ContentType.Application.OctetStream) {
                    file.inputStream().use { it.copyTo(this) }
                }
            }
        } else {
            // 没有范围请求，发送完整文件
            response.header(HttpHeaders.ContentLength, fileSize.toString())
            respondOutputStream(contentType = ContentType.Application.OctetStream) {
                file.inputStream().use { it.copyTo(this) }
            }
        }
    }

    /**
     * 配置服务器连接参数
     *
     * @param port 要监听的端口
     */
    private fun ApplicationEngine.Configuration.envConfig(port: Int) {
        connector {
            this.port = port
        }
    }

    /**
     * 配置文件共享功能
     *
     * 安装必要的Ktor插件并设置路由
     */
    private fun Application.configureFileSharing() {
        install(Compression) {
            gzip {
                priority = 1.0
            }
            deflate {
                priority = 10.0
                minimumSize(1024)
            }
        }

        // 使用共享的路由配置
        configureRoutes()
    }
}