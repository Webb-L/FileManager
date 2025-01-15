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
    fun replyList(message: SocketMessage) {
        socket.receive(message)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun replyRootPaths(message: SocketMessage) {
        MainScope().launch {
            val replyKey = (message.params["replyKey"] ?: "0").toLong()
            socket.replyMessage[replyKey] = ProtoBuf.decodeFromByteArray<List<PathInfo>>(message.body)
        }
    }


    fun replyTraversePath(message: SocketMessage) {
        socket.receive(message)
    }
}