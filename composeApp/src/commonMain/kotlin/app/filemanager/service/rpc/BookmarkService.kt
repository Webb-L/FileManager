package app.filemanager.service.rpc

import app.filemanager.data.main.DrawerBookmark
import app.filemanager.exception.AuthorityException
import app.filemanager.exception.toSocketResult
import app.filemanager.service.WebSocketResult
import app.filemanager.ui.state.device.DeviceCertificateState
import app.filemanager.utils.PathUtils
import kotlinx.rpc.annotations.Rpc
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Rpc
interface BookmarkService {
    suspend fun list(token: String): WebSocketResult<List<DrawerBookmark>>
}

class BookmarkServiceImpl() : BookmarkService, KoinComponent {
    private val deviceCertificateState: DeviceCertificateState by inject()

    // TODO 检查权限
    override suspend fun list(token: String): WebSocketResult<List<DrawerBookmark>> {
        if (deviceCertificateState.checkPermission(
                token,
                "bookmark",
                "read"
            )
        ) {
            return AuthorityException("对方没有为你设置权限").toSocketResult()
        }
        return WebSocketResult(value = PathUtils.getBookmarks())
    }
}