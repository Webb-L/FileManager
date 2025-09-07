package app.filemanager.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import java.net.Proxy

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
            }
        }
        @Suppress("UNCHECKED_CAST")
        (this as HttpClientConfig<OkHttpConfig>).config()
    }
