package app.filemanager.service.response

import app.filemanager.service.SocketClientManger
import app.filemanager.service.socket.SocketMessage

class FileResponse(private val socket: SocketClientManger) {
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
}