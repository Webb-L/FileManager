package app.filemanager.service.request

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.exception.EmptyDataException
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.service.SocketServerManger
import app.filemanager.service.WebSocketResult
import app.filemanager.service.socket.SocketMessage
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi

class PathRequest(private val socket: SocketServerManger) {
    // 远程设备需要我本地文件
    // TODO 检查权限
    @OptIn(ExperimentalSerializationApi::class)
    fun sendList(clientId: String, message: SocketMessage) {
        val directory = message.params["path"] ?: ""
        val fileAndFolder = directory.getFileAndFolder()
        if (fileAndFolder.isFailure) {
            MainScope().launch {
                val exceptionOrNull = fileAndFolder.exceptionOrNull() ?: EmptyDataException()
                socket.send(
                    clientId = clientId,
                    header = message.header.copy(command = "replyList"),
                    params = message.params,
                    body = WebSocketResult(
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
            socket.send(
                clientId = clientId,
                header = message.header.copy(command = "replyList"),
                params = message.params,
                body = WebSocketResult(
                    value = sendFileSimpleInfos
                )
            )
        }
    }

    // 远程设备需要我本地的书签
    // TODO 检查权限
    fun sendRootPaths(clientId: String, message: SocketMessage) {
        MainScope().launch {
            socket.send(
                clientId = clientId,
                header = message.header.copy(command = "replyRootPaths"),
                params = message.params,
                body = PathUtils.getRootPaths()
            )
        }
    }

    // 发送遍历的目录
    // TODO 检查权限
    fun sendTraversePath(clientId: String, message: SocketMessage) {
        val directory = message.params["path"] ?: ""
        val temp = mutableListOf<FileSimpleInfo>()
        PathUtils.traverse(directory) {
            temp.addAll(it)
        }

        MainScope().launch {
            socket.send(
                clientId = clientId,
                header = message.header.copy(command = "replyTraversePath"),
                params = message.params,
                body = temp
            )
        }
        println("结束")
    }
}