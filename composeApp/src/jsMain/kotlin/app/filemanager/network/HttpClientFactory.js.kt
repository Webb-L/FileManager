package app.filemanager.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js
import io.ktor.client.engine.js.JsClientConfig

/**
 * JS 平台实现：使用 JS 引擎（在浏览器环境中由浏览器网络栈处理）。
 * 不能显式控制代理，按默认行为执行，仅应用上层传入的配置。
 */
actual fun createNoProxyHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Js) {
        @Suppress("UNCHECKED_CAST")
        (this as HttpClientConfig<JsClientConfig>).config()
    }
