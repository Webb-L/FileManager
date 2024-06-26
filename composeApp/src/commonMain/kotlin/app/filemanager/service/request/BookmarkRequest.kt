package app.filemanager.service.request

import app.filemanager.service.WebSocketConnectService
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class BookmarkRequest(private val webSocketConnectService: WebSocketConnectService) {
    // 远程设备需要我本地的书签
    // TODO 检查权限
    fun sendBookmark(id: String, headerKey: Long) {
        val bookmarks = PathUtils.getBookmarks()
        MainScope().launch {
            webSocketConnectService.send(command = "/replyBookmark", header = "$id $headerKey", value = bookmarks)
        }
    }
}