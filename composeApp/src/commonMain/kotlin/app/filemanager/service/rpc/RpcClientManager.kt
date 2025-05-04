package app.filemanager.service.rpc

import app.filemanager.service.data.SocketDevice
import app.filemanager.service.handle.BookmarkHandle
import app.filemanager.service.handle.DeviceHandle
import app.filemanager.service.handle.FileHandle
import app.filemanager.service.handle.PathHandle
import io.ktor.client.*
import kotlinx.coroutines.cancel
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.protobuf.protobuf
import kotlinx.rpc.withService
import kotlinx.serialization.ExperimentalSerializationApi

enum class SocketClientIPEnum {
    ALL,
    IPV4_UP
}

class RpcClientManager {
    internal lateinit var deviceService: DeviceService
    internal lateinit var bookmarkService: BookmarkService
    internal lateinit var fileService: FileService
    internal lateinit var pathService: PathService
    internal var token: String = ""

    private val rpcClient by lazy { HttpClient { installKrpc() } }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun connect(connectDevice: SocketDevice) {
        val ktorRpcClient = rpcClient.rpc {
            url {
                host = connectDevice.host.replace("[", "").replace("]", "")
                port = connectDevice.port
            }
            rpcConfig {
                serialization {
                    protobuf()
                }
            }
        }
        deviceService = ktorRpcClient.withService<DeviceService>()

        token = deviceHandle.connect(this, connectDevice)

        bookmarkService = ktorRpcClient.withService<BookmarkService>()
        fileService = ktorRpcClient.withService<FileService>()
        pathService = ktorRpcClient.withService<PathService>()
    }

    fun disconnect(): Boolean {
        rpcClient.cancel()
        rpcClient.close()
        return true
    }

    val deviceHandle by lazy { DeviceHandle(this) }
    val bookmarkHandle by lazy { BookmarkHandle(this) }
    val pathHandle by lazy { PathHandle(this) }
    val fileHandle by lazy { FileHandle(this) }


    companion object {
        // 10 minutes in seconds
        const val CONNECT_TIMEOUT = 600

        const val PORT = 1204

        /**
         * 表示最大分片长度的常量，用于定义在数据传输过程中每个分片的最大长度。
         * 此值用于确保分片在网络传输时的稳定性和效率，避免超出可接受的大小限制。
         * 常量默认为 1024 * 6。
         */
        const val MAX_LENGTH = 1024 * 8 // 最大分片长度
    }
}