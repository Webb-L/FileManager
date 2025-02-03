package app.filemanager.service.handle

import app.filemanager.data.main.DrawerBookmark
import app.filemanager.service.rpc.BookmarkService

class BookmarkHandle(private val bookmarkService: BookmarkService) {
    suspend fun getBookmark(remoteId: String, replyCallback: (Result<List<DrawerBookmark>>) -> Unit) {
        val result = bookmarkService.list()
        replyCallback(Result.success(result))
    }
}