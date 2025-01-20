package app.filemanager.service.request

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.FileSizeInfo
import app.filemanager.exception.EmptyDataException
import app.filemanager.exception.ParameterErrorException
import app.filemanager.exception.toSocketResult
import app.filemanager.service.BaseSocketManager.Companion.MAX_LENGTH
import app.filemanager.service.SocketServerManger
import app.filemanager.service.WebSocketResult
import app.filemanager.service.data.RenameInfo
import app.filemanager.service.socket.SocketMessage
import app.filemanager.utils.FileUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf

class FileRequest(private val socket: SocketServerManger) {
    // 重命名文件和文件夹
    // TODO 检查权限
    @OptIn(ExperimentalSerializationApi::class)
    fun sendRename(clientId: String, message: SocketMessage) {
        var errorValue: WebSocketResult<Nothing>? = null
        var value: WebSocketResult<List<WebSocketResult<Boolean>>>? = null
        if (message.body.isEmpty()) {
            errorValue = ParameterErrorException().toSocketResult()
        }

        MainScope().launch {
            if (errorValue == null) {
                val renameInfos = ProtoBuf.decodeFromByteArray<List<RenameInfo>>(message.body)
                if (renameInfos.isEmpty()) {
                    errorValue = ParameterErrorException().toSocketResult()
                    return@launch
                }

                value = WebSocketResult(
                    value = renameInfos.map {
                        if (it.hasEmptyField()) {
                            return@map ParameterErrorException().toSocketResult()
                        }

                        val rename = FileUtils.rename(it.path, it.oldName, it.newName)
                        if (rename.isFailure) {
                            val exceptionOrNull = rename.exceptionOrNull() ?: EmptyDataException()
                            WebSocketResult(
                                exceptionOrNull.message,
                                exceptionOrNull::class.simpleName,
                                null
                            )
                        } else {
                            WebSocketResult(value = rename.getOrNull() ?: false)
                        }
                    }
                )
            }

            socket.send(
                clientId = clientId,
                header = message.header.copy(command = "replyRename"),
                params = message.params,
                value = errorValue ?: value!!
            )
        }
    }

    // 创建文件夹
    // TODO 检查权限
    @OptIn(ExperimentalSerializationApi::class)
    fun sendCreateFolder(clientId: String, message: SocketMessage) {
        var errorValue: WebSocketResult<Nothing>? = null
        var value: WebSocketResult<List<WebSocketResult<Boolean>>>? = null
        if (message.body.isEmpty()) {
            errorValue = ParameterErrorException().toSocketResult()
        }

        MainScope().launch {
            if (errorValue == null) {
                value = WebSocketResult(
                    value = ProtoBuf.decodeFromByteArray<List<String>>(message.body)
                        .map { path ->
                            val createFolder = FileUtils.createFolder(path)
                            if (createFolder.isFailure) {
                                val exceptionOrNull = createFolder.exceptionOrNull() ?: EmptyDataException()
                                WebSocketResult(
                                    exceptionOrNull.message,
                                    exceptionOrNull::class.simpleName,
                                    null
                                )
                            } else {
                                WebSocketResult(value = createFolder.getOrNull() ?: false)
                            }
                        })
            }

            socket.send(
                clientId = clientId,
                header = message.header.copy(command = "replyCreateFolder"),
                params = message.params,
                value = errorValue ?: value!!
            )
        }
    }

    // 获取文件和文件夹大小信息
    // TODO 检查权限
    @OptIn(ExperimentalSerializationApi::class)
    fun sendGetSizeInfo(clientId: String, message: SocketMessage) {
        val totalSpace: Long = (message.params["totalSpace"] ?: "-1").toLong()
        val freeSpace: Long = (message.params["freeSpace"] ?: "-1").toLong()
        var errorValue: WebSocketResult<Nothing>? = null
        var value: WebSocketResult<FileSizeInfo>? = null
        if (totalSpace <= -1L || freeSpace <= -1L) {
            errorValue = ParameterErrorException().toSocketResult()
        }
        MainScope().launch {
            if (errorValue == null) {
                try {
                    val fileSimpleInfo = ProtoBuf.decodeFromByteArray<FileSimpleInfo>(message.body)
                    value = WebSocketResult(value = fileSimpleInfo.getSizeInfo(totalSpace, freeSpace))
                } catch (e: Exception) {
                    errorValue = ParameterErrorException().toSocketResult()
                }
            }

            socket.send(
                clientId = clientId,
                header = message.header.copy(command = "replyGetSizeInfo"),
                params = message.params,
                value = errorValue ?: value!!
            )
        }
    }

    // 删除文件和文件夹
    // TODO 检查权限
    @OptIn(ExperimentalSerializationApi::class)
    fun sendDeleteFile(clientId: String, message: SocketMessage) {
        var errorValue: WebSocketResult<Nothing>? = null
        var value: WebSocketResult<List<Boolean>>? = null
        val paths = mutableListOf<String>()
        if (message.body.isEmpty()) {
            errorValue = ParameterErrorException().toSocketResult()
        } else {
            paths.addAll(ProtoBuf.decodeFromByteArray<List<String>>(message.body))
        }

        MainScope().launch {
            if (errorValue == null) {
                val list = paths.map {
                    val deleteFile = FileUtils.deleteFile(it)
                    deleteFile.isSuccess && deleteFile.getOrNull() ?: false
                }
                println("list = $list")
                value = WebSocketResult(value = list)
            }

            socket.send(
                clientId = clientId,
                header = message.header.copy(command = "replyDeleteFile"),
                params = message.params,
                value = errorValue ?: value!!
            )
        }
    }

    // 写入数据
    // TODO 检查权限
    // TODO 断点继传
    fun sendWriteBytes(clientId: String, message: SocketMessage) {
        val fileSize: Long = (message.params["fileSize"] ?: "-1").toLong()
        val blockIndex: Long = (message.params["blockIndex"] ?: "-1").toLong()
        val blockLength: Long = (message.params["blockLength"] ?: "-1").toLong()
        val path: String = (message.params["path"] ?: "")

        var errorValue: WebSocketResult<Nothing>? = null
        var value: WebSocketResult<Boolean>? = null

        // 参数验证
        if (fileSize < 0L || blockIndex < 0L || blockLength < 0L || path.isEmpty()) {
            errorValue = ParameterErrorException().toSocketResult()
        }
        MainScope().launch {
            if (errorValue == null) {
                val writeResult = FileUtils.writeBytes(path, fileSize, message.body, blockIndex * MAX_LENGTH)
                if (writeResult.isFailure) {
                    val exceptionOrNull = writeResult.exceptionOrNull() ?: EmptyDataException()
                    errorValue = WebSocketResult(
                        exceptionOrNull.message,
                        exceptionOrNull::class.simpleName,
                        null
                    )
                } else {
                    value = WebSocketResult(value = writeResult.getOrNull() ?: false)
                }
            }

            socket.send(
                clientId = clientId,
                header = message.header.copy(command = "replyWriteBytes"),
                params = message.params,
                value = errorValue ?: value!!
            )
        }
    }
}