package app.filemanager.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * 创建一个 HttpClient：在支持的平台（JVM/Android）禁用代理；
 * 其它平台使用各自引擎的默认行为。
 *
 * @param config 传入的配置块，用于安装插件、设置默认请求等。
 * @return 已配置的 HttpClient 实例。
 */
expect fun createNoProxyHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient
