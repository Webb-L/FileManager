package app.filemanager.service.request

import app.filemanager.service.WebSocketConnectService
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
}