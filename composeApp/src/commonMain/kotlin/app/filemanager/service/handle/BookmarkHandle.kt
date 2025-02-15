package app.filemanager.service.handle

import app.filemanager.data.main.DrawerBookmark
import app.filemanager.service.rpc.RpcClientManager

class BookmarkHandle(private val rpc: RpcClientManager) {
    suspend fun getBookmark(remoteId: String, replyCallback: (Result<List<DrawerBookmark>>) -> Unit) {
        val result = rpc.bookmarkService.list(rpc.token)
        if (result.isSuccess) {
            replyCallback(Result.success(result.value?: emptyList()))
        }else {
            replyCallback(Result.failure(result.deSerializable()))
        }
    }
}