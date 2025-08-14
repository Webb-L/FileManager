package app.filemanager.service.rpc

import app.filemanager.createSettings
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.FileSizeInfo
import app.filemanager.exception.AuthorityException
import app.filemanager.exception.EmptyDataException
import app.filemanager.exception.ParameterErrorException
import app.filemanager.exception.toSocketResult
import app.filemanager.service.WebSocketResult
import app.filemanager.service.data.RenameInfo
import app.filemanager.service.rpc.RpcClientManager.Companion.MAX_LENGTH
import app.filemanager.ui.state.device.DeviceCertificateState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.html.Entities
import kotlinx.rpc.annotations.Rpc
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Rpc
interface FileService {
    // 重命名文件和文件夹
    // TODO 检查权限
    suspend fun rename(token: String, renameInfos: List<RenameInfo>): WebSocketResult<List<WebSocketResult<Boolean>>>

    // 创建文件夹
    // TODO 检查权限
    suspend fun createFolder(token: String, names: List<String>): WebSocketResult<List<WebSocketResult<Boolean>>>

    // 获取文件和文件夹大小信息
    // TODO 检查权限
    suspend fun getSizeInfo(
        token: String,
        totalSpace: Long,
        freeSpace: Long,
        fileSimpleInfo: FileSimpleInfo
    ): WebSocketResult<FileSizeInfo>

    // 删除文件和文件夹
    // TODO 检查权限
    suspend fun delete(token: String, names: List<String>): WebSocketResult<List<WebSocketResult<Boolean>>>

    // 写入数据
    // TODO 检查权限
    // TODO 断点继传
    suspend fun writeBytes(
        token: String,
        fileSize: Long,
        blockIndex: Long,
        blockLength: Long,
        path: String,
        byteArray: ByteArray,
    ): WebSocketResult<Boolean>

    // 读取文件数据
    // TODO 检查权限
    fun readFileChunks(
        token: String,
        path: String,
        chunkSize: Long
    ): Flow<WebSocketResult<Pair<Long, ByteArray>>>

    // 获取文件信息
    // TODO 检查权限
    suspend fun getFileByPath(token: String, path: String): WebSocketResult<FileSimpleInfo>

    // 获取文件信息
    // TODO 检查权限
    suspend fun getFileByPathAndName(token: String, path: String, name: String): WebSocketResult<FileSimpleInfo>

    // 创建文件
    // TODO 检查权限
    suspend fun createFile(token: String, paths: List<String>): WebSocketResult<List<WebSocketResult<Boolean>>>
}

class FileServiceImpl() : FileService, KoinComponent {
    private val settings = createSettings()
    private val deviceCertificateState: DeviceCertificateState by inject()

    override suspend fun rename(
        token: String,
        renameInfos: List<RenameInfo>
    ): WebSocketResult<List<WebSocketResult<Boolean>>> {
        if (renameInfos.isEmpty()) {
            return ParameterErrorException().toSocketResult()
        }

        return WebSocketResult(
            value = renameInfos.map {
                if (deviceCertificateState.checkPermission(
                        token,
                        "${it.path}${PathUtils.getPathSeparator()}${it.oldName}",
                        "rename"
                    )
                ) {
                    return@map AuthorityException("对方没有为你设置权限").toSocketResult()
                }

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

    override suspend fun createFolder(
        token: String,
        names: List<String>
    ): WebSocketResult<List<WebSocketResult<Boolean>>> {
        if (names.isEmpty()) {
            return ParameterErrorException().toSocketResult()
        }

        return WebSocketResult(
            value = names
                .map { path ->
                    if (deviceCertificateState.checkPermission(
                            token,
                            path,
                            "write"
                        )
                    ) {
                        return@map AuthorityException("对方没有为你设置权限").toSocketResult()
                    }
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
        token: String,
        totalSpace: Long,
        freeSpace: Long,
        fileSimpleInfo: FileSimpleInfo
    ): WebSocketResult<FileSizeInfo> {
        if (deviceCertificateState.checkPermission(
                token,
                fileSimpleInfo.path,
                "read"
            )
        ) {
            return AuthorityException("对方没有为你设置权限").toSocketResult()
        }
        if (totalSpace <= -1L || freeSpace <= -1) {
            return ParameterErrorException().toSocketResult()
        }

        return WebSocketResult(value = fileSimpleInfo.getSizeInfo(totalSpace, freeSpace))
    }

    override suspend fun delete(token: String, names: List<String>): WebSocketResult<List<WebSocketResult<Boolean>>> {
        if (names.isEmpty()) {
            return ParameterErrorException().toSocketResult()
        }

        return WebSocketResult(
            value = names.map {path->
                if (deviceCertificateState.checkPermission(
                        token,
                        path,
                        "remove"
                    )
                ) {
                    return@map AuthorityException("对方没有为你设置权限").toSocketResult()
                }
                val rename = FileUtils.deleteFile(path)
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
        token: String,
        fileSize: Long,
        blockIndex: Long,
        blockLength: Long,
        path: String,
        byteArray: ByteArray,
    ): WebSocketResult<Boolean> {
        if (deviceCertificateState.checkPermission(
                token,
                path,
                "write"
            )
        ) {
            return AuthorityException("对方没有为你设置权限").toSocketResult()
        }
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

    override fun readFileChunks(
        token: String,
        path: String,
        chunkSize: Long
    ): Flow<WebSocketResult<Pair<Long, ByteArray>>> {
        return channelFlow {
            if (deviceCertificateState.checkPermission(
                    token,
                    path,
                    "read"
                )
            ) {
                send(AuthorityException("对方没有为你设置权限").toSocketResult())
                cancel()
                return@channelFlow
            }
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
                launch(Dispatchers.Default) {
                    send(sendData)
                }
            }
        }
    }

    override suspend fun getFileByPath(token: String, path: String): WebSocketResult<FileSimpleInfo> {
        if (deviceCertificateState.checkPermission(
                token,
                path,
                "read"
            )
        ) {
            return AuthorityException("对方没有为你设置权限").toSocketResult()
        }
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

    override suspend fun getFileByPathAndName(
        token: String,
        path: String,
        name: String
    ): WebSocketResult<FileSimpleInfo> {
        if (deviceCertificateState.checkPermission(
                token,
                "$path${PathUtils.getPathSeparator()}$name",
                "read"
            )
        ) {
            return AuthorityException("对方没有为你设置权限").toSocketResult()
        }
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

    override suspend fun createFile(
        token: String,
        paths: List<String>
    ): WebSocketResult<List<WebSocketResult<Boolean>>> {
        if (paths.isEmpty()) {
            return ParameterErrorException().toSocketResult()
        }

        return WebSocketResult(
            value = paths.map {
                if (deviceCertificateState.checkPermission(
                        token,
                        it,
                        "write"
                    )
                ) {
                    return AuthorityException("对方没有为你设置权限").toSocketResult()
                }
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