package app.filemanager.service.rpc

import app.filemanager.data.main.DrawerBookmark
import app.filemanager.utils.PathUtils
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc
import kotlin.coroutines.CoroutineContext

@Rpc
interface BookmarkService : RemoteService {
    suspend fun list(): List<DrawerBookmark>
}

class BookmarkServiceImpl(override val coroutineContext: CoroutineContext) : BookmarkService {
    override suspend fun list(): List<DrawerBookmark> {
        println(PathUtils.getBookmarks())
        return PathUtils.getBookmarks()
    }
}