package app.filemanager.service.response

import app.filemanager.data.file.PathInfo
import app.filemanager.service.SocketClientManger
import app.filemanager.service.socket.SocketMessage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf

class PathResponse(private val socket: SocketClientManger) {
    // 收到对方返回的文件文件夹信息
    @OptIn(ExperimentalSerializationApi::class)
    fun replyList(message: SocketMessage) {
        MainScope().launch {
            val replyKey = (message.params["replyKey"] ?: "0").toLong()
            val index = (message.params["index"] ?: "0").toLong()
            val count = (message.params["count"] ?: "0").toLong()
            if (index == 0L && count == 0L) {
                socket.replyMessage[replyKey] = Triple(index, count, message.body)
                return@launch
            }
            if (!socket.replyMessage.containsKey(replyKey)) {
                socket.replyMessage[replyKey] = Triple(
                    index,
                    count,
                    message.body
                )
            } else {
                val temp =
                    socket.replyMessage[replyKey] as Triple<Long, Long, ByteArray>

                socket.replyMessage[replyKey] = Triple(
                    index,
                    count,
                    (temp.third.toList() + message.body.toList()).toByteArray()
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun replyRootPaths(message: SocketMessage) {
        MainScope().launch {
            val replyKey = (message.params["replyKey"] ?: "0").toLong()
            socket.replyMessage[replyKey] = ProtoBuf.decodeFromByteArray<List<PathInfo>>(message.body)
        }
    }

    fun replyTraversePath(headerKey: Long, params: List<String>, content: String) {

    }
}