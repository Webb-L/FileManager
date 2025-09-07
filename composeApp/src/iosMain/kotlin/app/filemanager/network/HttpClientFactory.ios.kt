package app.filemanager.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig

/**
 * iOS 平台实现：使用 Darwin 引擎。当前 Darwin 未提供显式禁用代理的配置入口，
 * 因此沿用默认行为，仅应用上层传入的配置。
 */
actual fun createNoProxyHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Darwin) {
        @Suppress("UNCHECKED_CAST")
        (this as HttpClientConfig<DarwinClientEngineConfig>).config()
    }
