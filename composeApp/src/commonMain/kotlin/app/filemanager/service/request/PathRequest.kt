package app.filemanager.service.request

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.PathInfo
import app.filemanager.exception.EmptyDataException
import app.filemanager.exception.ParameterErrorException
import app.filemanager.exception.toSocketResult
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.service.SocketServerManger
import app.filemanager.service.WebSocketResult
import app.filemanager.service.socket.SocketMessage
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class PathRequest(private val socket: SocketServerManger) {
    // 远程设备需要我本地文件
    // TODO 检查权限
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
            socket.send(
                clientId = clientId,
                header = message.header.copy(command = "replyList"),
                params = message.params,
                value = WebSocketResult(
                    value = sendFileSimpleInfos
                )
            )
        }
    }

    // 远程设备需要我本地的书签
    // TODO 检查权限
    fun sendRootPaths(clientId: String, message: SocketMessage) {
        MainScope().launch {
            var errorValue: WebSocketResult<Nothing>? = null
            var value: WebSocketResult<List<PathInfo>>? = null

            val rootPaths = PathUtils.getRootPaths()
            if (rootPaths.isFailure) {
                val exceptionOrNull = rootPaths.exceptionOrNull() ?: EmptyDataException()
                errorValue = WebSocketResult(
                    exceptionOrNull.message,
                    exceptionOrNull::class.simpleName,
                    null
                )
            } else {
                value = WebSocketResult(value = rootPaths.getOrDefault(listOf()))
            }
            socket.send(
                clientId = clientId,
                header = message.header.copy(command = "replyRootPaths"),
                params = message.params,
                value = errorValue ?: value!!
            )
        }
    }

    // 发送遍历的目录
    // TODO 检查权限
    fun sendTraversePath(clientId: String, message: SocketMessage) {
        val path = message.params["path"] ?: ""
        if (path.isEmpty()) {
            MainScope().launch {
                socket.send(
                    clientId = clientId,
                    header = message.header.copy(command = "replyTraversePath"),
                    params = message.params,
                    value = ParameterErrorException().toSocketResult()
                )
            }
            return
        }


        PathUtils.traverse(path) { fileAndFolder ->
            var value: WebSocketResult<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>? = null
            var errorValue: WebSocketResult<Nothing>? = null
            if (fileAndFolder.isFailure) {
                val exceptionOrNull = fileAndFolder.exceptionOrNull() ?: EmptyDataException()
                errorValue = WebSocketResult(
                    exceptionOrNull.message,
                    exceptionOrNull::class.simpleName,
                    null
                )
            } else {
                value =
                    WebSocketResult(value = mutableMapOf<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>().apply {
                        fileAndFolder.getOrNull()?.forEach { fileSimpleInfo ->
                            val key = if (fileSimpleInfo.protocol == FileProtocol.Local)
                                Pair(FileProtocol.Device, fileSimpleInfo.protocolId)
                            else
                                Pair(fileSimpleInfo.protocol, fileSimpleInfo.protocolId)

                            if (!containsKey(key)) {
                                put(key, mutableListOf(fileSimpleInfo.apply {
                                    this.path = this.path.replace(path, "")
                                    this.protocol = FileProtocol.Local
                                    this.protocolId = ""
                                }))
                            } else {
                                get(key)?.add(fileSimpleInfo.apply {
                                    this.path = this.path.replace(path, "")
                                    this.protocol = FileProtocol.Local
                                    this.protocolId = ""
                                })
                            }
                        }
                    })
            }
            MainScope().launch {
                socket.send(
                    clientId = clientId,
                    header = message.header.copy(command = "replyTraversePath"),
                    params = message.params,
                    value = errorValue ?: value!!
                )
            }
        }
        println("结束")
    }
}