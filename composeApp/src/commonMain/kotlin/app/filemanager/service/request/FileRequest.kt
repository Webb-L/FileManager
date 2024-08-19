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
    fun sendCreateFolder(replyKey: Long,replyDeviceId: String, params: List<String>) {
        val command = "/replyCreateFolder"
        val header = listOf(replyKey.toString(), replyDeviceId)
        var value:Any
        if (params.size < 2) {
            value = WebSocketResult(value = ParameterErrorException())
        }

        val createFolder = FileUtils.createFolder(params[0], params[1])
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
            webSocketConnectService.send(
                command = command,
                header = header,
                value = value
            )
        }
    }
}