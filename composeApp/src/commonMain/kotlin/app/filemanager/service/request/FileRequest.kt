package app.filemanager.service.request

import app.filemanager.exception.EmptyDataException
import app.filemanager.exception.ParameterErrorException
import app.filemanager.service.SocketServerManger
import app.filemanager.service.WebSocketResult
import app.filemanager.service.socket.SocketMessage
import app.filemanager.utils.FileUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class FileRequest(private val socket: SocketServerManger) {
    // 重命名文件和文件夹
    // TODO 检查权限
    fun sendRename(clientId: String, message: SocketMessage) {
        MainScope().launch {
            val path = message.params["path"] ?: ""
            val oldName = message.params["oldName"] ?: ""
            val newName = message.params["newName"] ?: ""

            if (path.isEmpty() || oldName.isEmpty() || newName.isEmpty()) {
                val exception = ParameterErrorException()
                socket.send(
                    clientId = clientId,
                    header = message.header.copy(command = "replyRename"),
                    params = message.params,
                    body = WebSocketResult(
                        exception.message,
                        exception::class.simpleName,
                        null
                    )
                )
                return@launch
            }

            socket.send(
                clientId = clientId,
                header = message.header.copy(command = "replyRename"),
                params = message.params,
                body = WebSocketResult(
                    value = FileUtils.rename(path, oldName, newName)
                )
            )
        }
    }

    // 创建文件夹
    // TODO 检查权限
    fun sendCreateFolder(clientId: String, message: SocketMessage) {
        val path = message.params["path"] ?: ""
        val name = message.params["name"] ?: ""
        var value: Any
        if (path.isEmpty() || name.isEmpty()) {
            value = WebSocketResult(value = ParameterErrorException())
        }

        val createFolder = FileUtils.createFolder(path, name)
        if (createFolder.isFailure) {
            val exceptionOrNull = createFolder.exceptionOrNull() ?: EmptyDataException()
            value = WebSocketResult(
                exceptionOrNull.message,
                exceptionOrNull::class.simpleName,
                null
            )
        }

        value = WebSocketResult(value = createFolder.getOrNull() ?: false)

        MainScope().launch {
            socket.send(
                clientId = clientId,
                header = message.header.copy(command = "replyCreateFolder"),
                params = message.params,
                body = value
            )
        }
    }
}