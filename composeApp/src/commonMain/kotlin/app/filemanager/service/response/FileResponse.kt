package app.filemanager.service.response

import app.filemanager.service.SocketClientManger
import app.filemanager.service.WebSocketResult
import app.filemanager.service.socket.SocketMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf

class FileResponse(private val socket: SocketClientManger) {
    val writeErrorMaps = mutableMapOf<Long, MutableList<Long>>()
    fun replyRename(message: SocketMessage) {
        val replyKey = (message.params["replyKey"] ?: "0").toLong()
        socket.replyMessage[replyKey] = message.body
    }

    fun replyCreateFolder(message: SocketMessage) {
        val replyKey = (message.params["replyKey"] ?: "0").toLong()
        socket.replyMessage[replyKey] = message.body
    }

    fun replyGetSizeInfo(message: SocketMessage) {
        val replyKey = (message.params["replyKey"] ?: "0").toLong()
        socket.replyMessage[replyKey] = message.body
    }

    fun replyDeleteFile(message: SocketMessage) {
        val replyKey = (message.params["replyKey"] ?: "0").toLong()
        socket.replyMessage[replyKey] = message.body
    }

    @OptIn(ExperimentalSerializationApi::class, ExperimentalStdlibApi::class)
    fun replyWriteBytes(message: SocketMessage) {
        val replyKey = (message.params["replyKey"] ?: "0").toLong()
        val blockIndex: Long = (message.params["blockIndex"] ?: "-1").toLong()
        val blockLength: Long = (message.params["blockLength"] ?: "-1").toLong()
        val webSocketResult = ProtoBuf.decodeFromByteArray<WebSocketResult<Boolean>>(message.body)

        // TODO 记录错误的记录

        if (blockIndex + 1 == blockLength && webSocketResult.isSuccess && webSocketResult.value == true) {
            socket.replyMessage[replyKey] = writeErrorMaps[replyKey] ?: mutableListOf<Long>()
            writeErrorMaps.remove(replyKey)
        }

        if (!webSocketResult.isSuccess || webSocketResult.value == false) {
            writeErrorMaps.getOrPut(replyKey) { mutableListOf() }.add(blockIndex)
        }
    }
}