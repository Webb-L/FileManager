package app.filemanager.service.rpc

import app.filemanager.createSettings
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.FileSizeInfo
import app.filemanager.exception.EmptyDataException
import app.filemanager.exception.ParameterErrorException
import app.filemanager.exception.toSocketResult
import app.filemanager.service.WebSocketResult
import app.filemanager.service.data.RenameInfo
import app.filemanager.service.rpc.RpcClientManager.Companion.MAX_LENGTH
import app.filemanager.utils.FileUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
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

    // 读取文件数据
    // TODO 检查权限
    suspend fun readFileChunks(path: String, chunkSize: Long): Flow<WebSocketResult<Pair<Long, ByteArray>>>

    // 获取文件信息
    // TODO 检查权限
    suspend fun getFileByPath(path: String): WebSocketResult<FileSimpleInfo>

    // 获取文件信息
    // TODO 检查权限
    suspend fun getFileByPathAndName(path: String, name: String): WebSocketResult<FileSimpleInfo>

    // 创建文件
    // TODO 检查权限
    suspend fun createFile(paths: List<String>): WebSocketResult<List<WebSocketResult<Boolean>>>
}

class FileServiceImpl(override val coroutineContext: CoroutineContext) : FileService {
    private val settings = createSettings()

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

    override suspend fun readFileChunks(path: String, chunkSize: Long): Flow<WebSocketResult<Pair<Long, ByteArray>>> {
        return channelFlow {
            FileUtils.readFileChunks(path, chunkSize) { result ->
                val sendData = if (result.isSuccess) {
                    WebSocketResult(value = result.getOrDefault(Pair(0L, byteArrayOf())))
                } else {
                    val exceptionOrNull = result.exceptionOrNull() ?: EmptyDataException()
                    WebSocketResult(
                        exceptionOrNull.message,
                        exceptionOrNull::class.simpleName,
                        null
                    )
                }
                launch {
                    send(sendData)
                }
            }
        }
    }

    override suspend fun getFileByPath(path: String): WebSocketResult<FileSimpleInfo> {
        if (path.isEmpty()) {
            return ParameterErrorException().toSocketResult()
        }

        val result = FileUtils.getFile(path)
        if (result.isFailure) {
            val exceptionOrNull = result.exceptionOrNull() ?: EmptyDataException()
            return WebSocketResult(
                exceptionOrNull.message,
                exceptionOrNull::class.simpleName,
                null
            )
        }

        return WebSocketResult(value = result.getOrNull()?.apply {
            protocol = FileProtocol.Device
            protocolId = settings.getString("deviceId", "")
        }!!)
    }

    override suspend fun getFileByPathAndName(path: String, name: String): WebSocketResult<FileSimpleInfo> {
        if (path.isEmpty() || name.isEmpty()) {
            return ParameterErrorException().toSocketResult()
        }

        val result = FileUtils.getFile(path, name)
        if (result.isFailure) {
            val exceptionOrNull = result.exceptionOrNull() ?: EmptyDataException()
            return WebSocketResult(
                exceptionOrNull.message,
                exceptionOrNull::class.simpleName,
                null
            )
        }

        return WebSocketResult(value = result.getOrNull()?.apply {
            protocol = FileProtocol.Device
            protocolId = settings.getString("deviceId", "")
        }!!)
    }

    override suspend fun createFile(paths: List<String>): WebSocketResult<List<WebSocketResult<Boolean>>> {
        if (paths.isEmpty()) {
            return ParameterErrorException().toSocketResult()
        }

        return WebSocketResult(
            value = paths.map {
                val file = FileUtils.createFile(it)
                if (file.isFailure) {
                    val exceptionOrNull = file.exceptionOrNull() ?: EmptyDataException()
                    WebSocketResult(
                        exceptionOrNull.message,
                        exceptionOrNull::class.simpleName,
                        null
                    )
                } else {
                    WebSocketResult(value = file.getOrNull() ?: false)
                }
            }
        )
    }
}