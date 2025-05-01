package app.filemanager.service

import app.filemanager.ui.state.file.FileShareState
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.compression.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

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