package app.filemanager.service.request

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.exception.EmptyDataException
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.service.WebSocketConnectService
import app.filemanager.service.WebSocketResult
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class PathRequest(private val webSocketConnectService: WebSocketConnectService) {
    // 远程设备需要我本地文件
    // TODO 检查权限
    fun sendList(id: String, replyKey: Long, directory: String) {
        val fileAndFolder = directory.getFileAndFolder()
        if (fileAndFolder.isFailure) {
            MainScope().launch {
                val exceptionOrNull = fileAndFolder.exceptionOrNull() ?: EmptyDataException()
                webSocketConnectService.send(
                    command = "/replyList",
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

        val sendFileSimpleInfos = mutableMapOf<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>().apply {
            fileAndFolder.getOrNull()?.forEach { fileSimpleInfo ->
                val key = if (fileSimpleInfo.protocol == FileProtocol.Local)
                    Pair(FileProtocol.Device, fileSimpleInfo.protocolId)
                else
                    Pair(fileSimpleInfo.protocol, fileSimpleInfo.protocolId)

                if (!containsKey(key)) {
                    put(key, mutableListOf(fileSimpleInfo.apply {
                        this.path = this.path.replace(directory, "")
                        this.protocol = FileProtocol.Local
                        this.protocolId = ""
                    }))
                } else {
                    get(key)?.add(fileSimpleInfo.apply {
                        this.path = this.path.replace(directory, "")
                        this.protocol = FileProtocol.Local
                        this.protocolId = ""
                    })
                }
            }
        }

        MainScope().launch {
            webSocketConnectService.send(
                command = "/replyList",
                header = listOf(id, replyKey.toString()),
                value = WebSocketResult(
                    value = sendFileSimpleInfos
                )
            )
        }
    }

    // 远程设备需要我本地的书签
    // TODO 检查权限
    fun sendRootPaths(id: String, replyKey: Long) {
        MainScope().launch {
            val rootPaths = PathUtils.getRootPaths()
            webSocketConnectService.send(
                command = "/replyRootPaths",
                header = listOf(id, replyKey.toString()),
                value = rootPaths
            )
        }
    }

    // 发送遍历的目录
    // TODO 检查权限
    fun sendTraversePath(params: List<String>, replyKey: Long) {
        PathUtils.traverse(params[0]) {
            MainScope().launch {
                webSocketConnectService.send(
                    command = "/replyTraversePath",
                    header = listOf("",replyKey.toString()),
                    value = it
                )
            }
        }
    }
}