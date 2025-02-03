package app.filemanager.service.rpc

import app.filemanager.service.data.SocketDevice
import app.filemanager.service.handle.BookmarkHandle
import app.filemanager.service.handle.DeviceHandle
import app.filemanager.service.handle.FileHandle
import app.filemanager.service.handle.PathHandle
import io.ktor.client.*
import io.ktor.utils.io.core.*
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
        bookmarkService = ktorRpcClient.withService<BookmarkService>()
        fileService = ktorRpcClient.withService<FileService>()
        pathService = ktorRpcClient.withService<PathService>()

        deviceHandle.connect(this, connectDevice)
    }

    fun disconnect(): Boolean {
        rpcClient.cancel()
        rpcClient.close()
        return true
    }

    val deviceHandle by lazy { DeviceHandle(deviceService) }
    val bookmarkHandle by lazy { BookmarkHandle(bookmarkService) }
    val pathHandle by lazy { PathHandle(this) }
    val fileHandle by lazy { FileHandle(fileService) }


    companion object {
        // 10 minutes in seconds
        const val CONNECT_TIMEOUT = 600

        const val PORT = 1204

        val SEND_IDENTIFIER = "--FileManager--bytearray".toByteArray()

        const val SEND_LENGTH = 1024 * 8

        /**
         * 表示最大分片长度的常量，用于定义在数据传输过程中每个分片的最大长度。
         * 此值用于确保分片在网络传输时的稳定性和效率，避免超出可接受的大小限制。
         * 常量默认为 1024 * 6。
         */
        const val MAX_LENGTH = 1024 * 6 // 最大分片长度

        /**
         * 定义用于管理请求超时的常量。
         *
         * 该常量表示超时时间，单位为毫秒，典型场景是用于方法中循环等待某些条件完成的时间限制。
         * 利用此超时时间，避免因条件未满足而导致程序无法继续执行的问题。
         *
         * 使用范围：
         * - 在等待异步操作结果时，用于计算当前时间与超时时间之间的差值。
         * - 配合检查机制，例如 `waitFinish` 方法，处理客户端或服务端的响应超时问题。
         *
         * 常量值定义为 10,000 毫秒（即 10 秒），具体数值可根据实际需求调整。
         */
        const val TIMEOUT = 10_000L // 超时时间
    }
}