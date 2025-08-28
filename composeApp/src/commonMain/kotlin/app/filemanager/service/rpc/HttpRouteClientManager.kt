package app.filemanager.service.rpc

import app.filemanager.data.main.DeviceConnectType
import app.filemanager.db.FileManagerDatabase
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.DeviceConnectRequest
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.httproute.BookmarkRouteClient
import app.filemanager.service.rpc.httproute.DeviceRouteClient
import app.filemanager.service.rpc.httproute.FileRouteClient
import app.filemanager.service.rpc.httproute.PathRouteClient
import app.filemanager.ui.state.main.DeviceState
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.protobuf.*
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HttpRouteClientManager : KoinComponent {
    private val deviceState by inject<DeviceState>()
    private val database by inject<FileManagerDatabase>()

    internal lateinit var deviceRouteClient: DeviceRouteClient
    internal lateinit var bookmarkRouteClient: BookmarkRouteClient
    internal lateinit var fileRouteClient: FileRouteClient
    internal lateinit var pathRouteClient: PathRouteClient
    internal var token: String = ""

    // 共享的 HttpClient 实例
    @OptIn(ExperimentalSerializationApi::class)
    private val sharedHttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                protobuf()
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun connect(connectDevice: SocketDevice) {
        // 构建基础URL
        val baseUrl = "http://${connectDevice.host.replace("[", "").replace("]", "")}:${connectDevice.port}"

        // 使用共享的HttpClient初始化所有路由客户端
        deviceRouteClient = DeviceRouteClient(baseUrl, token, sharedHttpClient)

        val result = deviceRouteClient.connectDevice(DeviceConnectRequest(connectDevice))
        if (result.isFailure) {
            throw result.exceptionOrNull()!!
        }
        token = result.getOrThrow().token

        deviceState.socketDevices.indexOfFirst { it.id == connectDevice.id }.takeIf { it >= 0 }?.let { index ->
            val socketDevice = connectDevice.withCopy(
                connectType = if (result.getOrThrow().connectType == DeviceConnectType.APPROVED)
                    ConnectType.Connect
                else
                    ConnectType.Fail,
                token = result.getOrThrow().token,
                httpClient = this,
            )
            deviceState.socketDevices[index] = socketDevice

            if (socketDevice.connectType == ConnectType.Connect) {
                deviceState.devices.add(socketDevice.toDevice())
            }
        }


        bookmarkRouteClient = BookmarkRouteClient(baseUrl, token, sharedHttpClient)
        fileRouteClient = FileRouteClient(baseUrl, token, sharedHttpClient)
        pathRouteClient = PathRouteClient(baseUrl, token, sharedHttpClient)
    }

    fun disconnect(): Boolean {
        // 关闭共享的 HttpClient
        sharedHttpClient.close()
        return true
    }

    companion object {
        // 10 minutes in seconds
        const val CONNECT_TIMEOUT = 600

        const val PORT = 12040

        /**
         * 表示最大分片长度的常量，用于定义在数据传输过程中每个分片的最大长度。
         * 此值用于确保分片在网络传输时的稳定性和效率，避免超出可接受的大小限制。
         * 常量默认为 1024 * 8。
         */
        const val MAX_LENGTH = 1024 * 8 // 最大分片长度
    }
}

enum class SocketClientIPEnum {
    ALL,
    IPV4_UP
}