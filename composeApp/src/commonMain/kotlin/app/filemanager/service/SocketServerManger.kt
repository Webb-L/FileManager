package app.filemanager.service

import app.filemanager.extensions.chunked
import app.filemanager.service.request.PathRequest
import app.filemanager.service.socket.SocketHeader
import app.filemanager.service.socket.SocketMessage
import app.filemanager.service.socket.SocketServer
import app.filemanager.service.socket.createSocketServer
import app.filemanager.ui.state.main.DeviceState
import kotlinx.coroutines.MainScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SocketServerManger : KoinComponent {
    private val socket: SocketServer = createSocketServer()
    internal val replyMessage = mutableMapOf<Long, Any>()
    internal val deviceState by inject<DeviceState>()
    private val mainScope = MainScope()

    private val pathRequest by lazy { PathRequest(this) }

    suspend fun connect(port: Int = 1204) {
        socket.start(port) { clientId, message ->
            println("server = ${message.header.command}")
            when (message.header.command) {
                "rootPaths" -> pathRequest.sendRootPaths(clientId, message)
                "list" -> pathRequest.sendList(clientId, message)

                else -> {
                    println("未能匹配上：$message")
                }
            }
        }
    }

    /**
     * 发送消息
     * 使用：send(指令, 设备1,设备2,... 消息标识, 参数, 发送的消息)
     */
    @OptIn(ExperimentalSerializationApi::class)
    internal suspend inline fun <reified T> send(
        clientId: String,
        header: SocketHeader,
        params: Map<String, String> = mapOf(),
        body: T
    ) {
        val byteArray = ProtoBuf.encodeToByteArray(body)

        if (byteArray.size < MAX_LENGTH) {
            val encodeToByteArray = ProtoBuf.encodeToByteArray(
                SocketMessage(
                    header = header,
                    params = params,
                    body = byteArray
                )
            )
            println("server = ---------${encodeToByteArray.size}---------")
            socket.send(
                clientId,
                encodeToByteArray
            )

//
//            println(
//                "------------------- >>>>>>\nheader = $command [$headerString]\nparams = [$paramsString]\ncontent = [$content]\n",
//            )
            return
        }

        var index = 1
        val chunked = byteArray.chunked(MAX_LENGTH)
        chunked.forEach {
            val socketMessage = SocketMessage(
                header = header,
                params = params + mapOf("index" to index.toString(), "count" to chunked.size.toString()),
                body = it
            )
            println("header = ${socketMessage.header} params = ${socketMessage.params} it=${it.size}")

            socket.send(
                clientId,
                ProtoBuf.encodeToByteArray(
                    socketMessage
                )
            )
//            println(
//                "------------------- >>>>>>\nheader = $command [$headerString]\nparams = [$paramsString]\ncontent = [$it]\n",
//            )
            index++
        }
    }
}