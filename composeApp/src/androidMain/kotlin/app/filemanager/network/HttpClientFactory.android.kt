package app.filemanager.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import java.net.Proxy

/**
 * Android 平台实现：使用 OkHttp 引擎并禁用代理（Proxy.NO_PROXY），
 * 避免请求被系统/网络设置的代理劫持或改变路由。
 */
actual fun createNoProxyHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(OkHttp) {
        engine {
            // 在 Android 上禁用系统或环境代理
            config {
                proxy(Proxy.NO_PROXY)
            }
        }
        @Suppress("UNCHECKED_CAST")
        (this as HttpClientConfig<OkHttpConfig>).config()
    }
