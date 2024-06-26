package app.filemanager.service.request

import app.filemanager.exception.EmptyDataException
import app.filemanager.exception.ParameterErrorException
import app.filemanager.service.WebSocketConnectService
import app.filemanager.service.WebSocketResult
import app.filemanager.utils.FileUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class FileRequest(private val webSocketConnectService: WebSocketConnectService) {
    // 重命名文件和文件夹
    // TODO 检查权限
    fun sendRename(content: String) {
        MainScope().launch {
            val renameArgs = content.split(" ")
            if (renameArgs.size > 2)
                FileUtils.rename(renameArgs[0], renameArgs[1], renameArgs[2])
        }
    }

    // 创建文件夹
    // TODO 检查权限
    fun sendCreateFolder(id: String, replyKey: Long, params: List<String>) {
        val command = "/replyCreateFolder"
        if (params.size < 2) {
            MainScope().launch {
                webSocketConnectService.send(
                    command = command,
                    header = listOf(id, replyKey.toString()),
                    value = WebSocketResult(
                        value = ParameterErrorException()
                    )
                )
            }
            return
        }

        val createFolder = FileUtils.createFolder(params[0], params[1])
        if (createFolder.isFailure) {
            MainScope().launch {
                val exceptionOrNull = createFolder.exceptionOrNull() ?: EmptyDataException()
                webSocketConnectService.send(
                    command = command,
                    header = listOf(id, replyKey.toString()),
                    value = WebSocketResult(
                        exceptionOrNull.message,
                        exceptionOrNull::class.simpleName,
                        null
                    )
                )
            }
            return
        }

        MainScope().launch {
            webSocketConnectService.send(
                command = command,
                header = listOf(id, replyKey.toString()),
                value = WebSocketResult(
                    value = createFolder.getOrNull() ?: false
                )
            )
        }
    }
}