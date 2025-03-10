package app.filemanager.service.rpc

import app.filemanager.service.data.SocketDevice
import app.filemanager.service.handle.ShareHandle
import io.ktor.client.*
import kotlinx.coroutines.cancel
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.protobuf.protobuf
import kotlinx.rpc.withService
import kotlinx.serialization.ExperimentalSerializationApi

class RpcShareClientManager {
    internal lateinit var shareService: ShareService
    internal var token: String = ""

    private val rpcClient by lazy { HttpClient { installKrpc() } }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun share(connectDevice: SocketDevice) {
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
        shareService = ktorRpcClient.withService<ShareService>()
        shareHandle.connect(this, connectDevice)
    }

    fun disconnect(): Boolean {
        rpcClient.cancel()
        rpcClient.close()
        return true
    }

    val shareHandle by lazy { ShareHandle(this) }
}