package app.filemanager.service.request

import app.filemanager.service.SocketServerManger
import app.filemanager.service.WebSocketResult
import app.filemanager.service.socket.SocketMessage
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class BookmarkRequest(private val socket: SocketServerManger) {
    // 远程设备需要我本地的书签
    // TODO 检查权限
    fun sendBookmark(clientId: String, message: SocketMessage) {
        val bookmarks = PathUtils.getBookmarks()
        MainScope().launch {
            socket.send(
                clientId = clientId,
                header = message.header.copy(command = "replyBookmark"),
                params = message.params,
                body = WebSocketResult(
                    value = bookmarks
                )
            )
        }
    }
}