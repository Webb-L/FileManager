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

}