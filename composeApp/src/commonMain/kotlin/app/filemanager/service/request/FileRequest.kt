package app.filemanager.service.request

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.FileSizeInfo
import app.filemanager.exception.EmptyDataException
import app.filemanager.exception.ParameterErrorException
import app.filemanager.exception.toSocketResult
import app.filemanager.service.SocketServerManger
import app.filemanager.service.WebSocketResult
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
    fun sendRename(clientId: String, message: SocketMessage) {
        val path = message.params["path"] ?: ""
        val oldName = message.params["oldName"] ?: ""
        val newName = message.params["newName"] ?: ""

        var errorValue: WebSocketResult<Nothing>? = null
        var value: WebSocketResult<Boolean>? = null
        if (path.isEmpty() || oldName.isEmpty() || newName.isEmpty()) {
            errorValue = ParameterErrorException().toSocketResult()
        }

        MainScope().launch {
            if (errorValue == null) {
                value = WebSocketResult(
                    value = FileUtils.rename(path, oldName, newName)
                )
            }

            socket.send(
                clientId = clientId,
                header = message.header.copy(command = "replyRename"),
                params = message.params,
                body = errorValue ?: value!!
            )
        }
    }

    // 创建文件夹
    // TODO 检查权限
    fun sendCreateFolder(clientId: String, message: SocketMessage) {
        val path = message.params["path"] ?: ""
        val name = message.params["name"] ?: ""
        var errorValue: WebSocketResult<Nothing>? = null
        var value: WebSocketResult<Boolean>? = null
        if (path.isEmpty() || name.isEmpty()) {
            errorValue = ParameterErrorException().toSocketResult()
        }

        MainScope().launch {
            if (errorValue == null) {
                val createFolder = FileUtils.createFolder(path, name)
                if (createFolder.isFailure) {
                    val exceptionOrNull = createFolder.exceptionOrNull() ?: EmptyDataException()
                    errorValue = WebSocketResult(
                        exceptionOrNull.message,
                        exceptionOrNull::class.simpleName,
                        null
                    )
                } else {
                    value = WebSocketResult(value = createFolder.getOrNull() ?: false)
                }
            }

            socket.send(
                clientId = clientId,
                header = message.header.copy(command = "replyCreateFolder"),
                params = message.params,
                body = errorValue ?: value!!
            )
        }
    }

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
                body = errorValue ?: value!!
            )
        }
    }
}