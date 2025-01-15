package app.filemanager.service

import app.filemanager.service.request.BookmarkRequest
import app.filemanager.service.request.FileRequest
import app.filemanager.service.request.PathRequest
import app.filemanager.service.socket.SocketServer
import app.filemanager.service.socket.createSocketServer
import org.koin.core.component.KoinComponent

class SocketServerManger : KoinComponent, BaseSocketManager("server") {
    override val socket: SocketServer = createSocketServer()

    private val pathRequest by lazy { PathRequest(this) }
    private val fileRequest by lazy { FileRequest(this) }
    private val bookmarkRequest by lazy { BookmarkRequest(this) }

    suspend fun connect(port: Int = 1204) {
        socket.start(port) { clientId, message ->
            when (message.header.command) {
                "rootPaths" -> pathRequest.sendRootPaths(clientId, message)
                "list" -> pathRequest.sendList(clientId, message)
                "traversePath" -> pathRequest.sendTraversePath(clientId, message)
                "bookmark" -> bookmarkRequest.sendBookmark(clientId, message)
                "rename" -> fileRequest.sendRename(clientId, message)
                "createFolder" -> fileRequest.sendCreateFolder(clientId, message)
                "getSizeInfo" -> fileRequest.sendGetSizeInfo(clientId, message)
                "deleteFile" -> fileRequest.sendDeleteFile(clientId, message)
                "writeBytes" -> fileRequest.sendWriteBytes(clientId, message)

                else -> {
                    println("【server】未能匹配上： header = ${message.header} params = ${message.params} it=${message.body.size}")
                }
            }
        }
    }
}