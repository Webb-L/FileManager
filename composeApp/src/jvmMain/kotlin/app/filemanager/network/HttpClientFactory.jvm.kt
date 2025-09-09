package app.filemanager.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * JVM 平台实现：使用 OkHttp 引擎并显式禁用代理（Proxy.NO_PROXY）。
 * 这样可避免读取系统/环境代理导致的请求走代理。
 */
actual fun createNoProxyHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(OkHttp) {
        engine {
            // 禁用系统或环境代理
            config {
                proxy(Proxy.NO_PROXY)
                // 提升并发连接与队列能力
                dispatcher(Dispatcher().apply {
                    maxRequests = 64
                    maxRequestsPerHost = 64
                })
                // 扩大连接池以减少连接重建
                connectionPool(ConnectionPool(64, 5, TimeUnit.MINUTES))
                // 合理的超时设置（可按需调整）
                connectTimeout(30, TimeUnit.SECONDS)
                readTimeout(5, TimeUnit.MINUTES)
                writeTimeout(5, TimeUnit.MINUTES)
                retryOnConnectionFailure(true)
            }
        }
        @Suppress("UNCHECKED_CAST")
        this.config()
    }
