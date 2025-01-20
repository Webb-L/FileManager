package app.filemanager.service.response

import app.filemanager.service.SocketClientManger
import app.filemanager.service.socket.SocketMessage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class BookmarkResponse(private val socket: SocketClientManger) {
    fun replyBookmark(message: SocketMessage) {
        MainScope().launch {
            val replyKey = (message.params["replyKey"] ?: "0").toLong()
            socket.replyMessage[replyKey] = Pair(-2, message.body)
        }
    }
}