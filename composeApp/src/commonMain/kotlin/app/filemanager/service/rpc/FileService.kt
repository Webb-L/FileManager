package app.filemanager.service.rpc

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.FileSizeInfo
import app.filemanager.exception.EmptyDataException
import app.filemanager.exception.ParameterErrorException
import app.filemanager.exception.toSocketResult
import app.filemanager.service.BaseSocketManager.Companion.MAX_LENGTH
import app.filemanager.service.WebSocketResult
import app.filemanager.service.data.RenameInfo
import app.filemanager.utils.FileUtils
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc
import kotlin.coroutines.CoroutineContext

@Rpc
interface FileService : RemoteService {
    // 重命名文件和文件夹
    // TODO 检查权限
    suspend fun rename(renameInfos: List<RenameInfo>): WebSocketResult<List<WebSocketResult<Boolean>>>

    // 创建文件夹
    // TODO 检查权限
    suspend fun createFolder(names: List<String>): WebSocketResult<List<WebSocketResult<Boolean>>>

    // 获取文件和文件夹大小信息
    // TODO 检查权限
    suspend fun getSizeInfo(
        totalSpace: Long,
        freeSpace: Long,
        fileSimpleInfo: FileSimpleInfo
    ): WebSocketResult<FileSizeInfo>

    // 删除文件和文件夹
    // TODO 检查权限
    suspend fun delete(names: List<String>): WebSocketResult<List<WebSocketResult<Boolean>>>

    // 写入数据
    // TODO 检查权限
    // TODO 断点继传
    suspend fun writeBytes(
        fileSize: Long,
        blockIndex: Long,
        blockLength: Long,
        path: String,
        byteArray: ByteArray,
    ): WebSocketResult<Boolean>
}

class FileServiceImpl(override val coroutineContext: CoroutineContext) : FileService {
    override suspend fun rename(renameInfos: List<RenameInfo>): WebSocketResult<List<WebSocketResult<Boolean>>> {
        if (renameInfos.isEmpty()) {
            return ParameterErrorException().toSocketResult()
        }

        return WebSocketResult(
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

    override suspend fun createFolder(names: List<String>): WebSocketResult<List<WebSocketResult<Boolean>>> {
        if (names.isEmpty()) {
            return ParameterErrorException().toSocketResult()
        }

        return WebSocketResult(
            value = names
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

    override suspend fun getSizeInfo(
        totalSpace: Long,
        freeSpace: Long,
        fileSimpleInfo: FileSimpleInfo
    ): WebSocketResult<FileSizeInfo> {
        println(totalSpace)
        if (totalSpace <= -1L || freeSpace <= -1) {
            return ParameterErrorException().toSocketResult()
        }

        return WebSocketResult(value = fileSimpleInfo.getSizeInfo(totalSpace, freeSpace))
    }

    override suspend fun delete(names: List<String>): WebSocketResult<List<WebSocketResult<Boolean>>> {
        if (names.isEmpty()) {
            return ParameterErrorException().toSocketResult()
        }

        return WebSocketResult(
            value = names.map {
                val rename = FileUtils.deleteFile(it)
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

    override suspend fun writeBytes(
        fileSize: Long,
        blockIndex: Long,
        blockLength: Long,
        path: String,
        byteArray: ByteArray,
    ): WebSocketResult<Boolean> {
        if (fileSize < 0L || blockIndex < 0L || blockLength < 0L || path.isEmpty()) {
            return ParameterErrorException().toSocketResult()
        }

        val writeResult = FileUtils.writeBytes(path, fileSize, byteArray, blockIndex * MAX_LENGTH)
        if (writeResult.isFailure) {
            val exceptionOrNull = writeResult.exceptionOrNull() ?: EmptyDataException()
            return WebSocketResult(
                exceptionOrNull.message,
                exceptionOrNull::class.simpleName,
                null
            )
        }

        return WebSocketResult(value = writeResult.getOrNull() ?: false)
    }
}