package app.filemanager.service.handle

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.FileSizeInfo
import app.filemanager.exception.EmptyDataException
import app.filemanager.service.data.RenameInfo
import app.filemanager.service.rpc.FileService
import app.filemanager.service.rpc.RpcClientManager
import app.filemanager.service.rpc.RpcClientManager.Companion.MAX_LENGTH
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.streamScoped
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.ceil

class FileHandle(private val rpc: RpcClientManager) : KoinComponent {
    private val deviceState: DeviceState by inject()

    /**
     * 重命名文件和文件夹
     *
     * @param remoteId
     * @param path
     * @param oldName
     * @param newName
     */
    suspend fun rename(
        remoteId: String,
        path: String,
        oldName: String,
        newName: String,
        replyCallback: (Result<Boolean>) -> Unit
    ) {
        val result = rpc.fileService.rename(
            rpc.token,
            renameInfos = listOf(RenameInfo(path, oldName, newName))
        )
        if (!result.isSuccess) {
            replyCallback(Result.failure(result.deSerializable()))
            return
        }


        // 若解码结果成功，则获取内层列表
        val webSocketResults = result.value.orEmpty()
        if (webSocketResults.isEmpty()) {
            replyCallback(Result.failure(Exception("重命名失败")))
            return
        }

        // 获取列表中的第一个元素并判断其是否成功
        val firstResult = webSocketResults.first()
        if (firstResult.isSuccess) {
            replyCallback(Result.success(firstResult.value ?: false))
        } else {
            replyCallback(Result.failure(firstResult.deSerializable()))
        }
    }

    /**
     * 创建文件夹
     *
     * @param remoteId
     * @param path
     * @param name
     * @param replyCallback
     * @receiver
     */
    suspend fun createFolder(remoteId: String, path: String, name: String, replyCallback: (Result<Boolean>) -> Unit) {
        val result = rpc.fileService.createFolder(
            rpc.token,
            listOf("$path${PathUtils.getPathSeparator()}$name")
        )
        if (!result.isSuccess) {
            replyCallback(Result.failure(result.deSerializable()))
            return
        }

        val resultList = result.value.orEmpty()
        if (resultList.isNotEmpty()) {
            val first = resultList.first()
            if (first.isSuccess) {
                replyCallback(Result.success(first.value ?: false))
            } else {
                replyCallback(Result.failure(first.deSerializable()))
            }
        } else {
            replyCallback(Result.failure(Exception("文件夹创建失败")))
        }
    }

    suspend fun getFileSizeInfo(
        id: String,
        fileSimpleInfo: FileSimpleInfo,
        totalSpace: Long,
        freeSpace: Long,
        replyCallback: (Result<FileSizeInfo>) -> Unit
    ) {
        val result = rpc.fileService.getSizeInfo(
            rpc.token,
            totalSpace,
            freeSpace,
            fileSimpleInfo
        )
        if (!result.isSuccess) {
            replyCallback(Result.failure(result.deSerializable()))
            return
        }
        replyCallback(Result.success(result.value as FileSizeInfo))
    }

    suspend fun deleteFile(id: String, paths: List<String>, replyCallback: (Result<List<Boolean>>) -> Unit) {
        val result = rpc.fileService.delete(
            rpc.token,
            paths
        )

        if (!result.isSuccess) {
            replyCallback(Result.failure(result.deSerializable()))
            return
        }

        replyCallback(Result.success(result.value.orEmpty().map { it.value ?: false }))
    }

    suspend fun writeBytes(
        id: String,
        srcFileSimpleInfo: FileSimpleInfo,
        destFileSimpleInfo: FileSimpleInfo,
        fileSimpleInfo: FileSimpleInfo,
        replyCallback: (Result<Boolean>) -> Unit
    ) {
        val srcFileSimpleInfoPath =
            if (srcFileSimpleInfo.isDirectory)
                "${srcFileSimpleInfo.path}${fileSimpleInfo.path}"
            else
                srcFileSimpleInfo.path

        val destFileSimpleInfoPath =
            if (srcFileSimpleInfo.isDirectory)
                "${destFileSimpleInfo.path}${fileSimpleInfo.path}"
            else
                destFileSimpleInfo.path

        var length = ceil(fileSimpleInfo.size / MAX_LENGTH.toFloat()).toLong()
        val mainScope = MainScope()

        // 本地 to 远程
        if (srcFileSimpleInfo.protocol == FileProtocol.Local && destFileSimpleInfo.protocol == FileProtocol.Device) {
            if (fileSimpleInfo.size == 0L) {
                val createFile = rpc.fileService.createFile(
                    rpc.token,
                    listOf(destFileSimpleInfoPath)
                )
                if (createFile.isSuccess) {
                    replyCallback(Result.success(createFile.value?.first()?.value == true))
                } else {
                    replyCallback(Result.failure(createFile.deSerializable()))
                }
                return
            }

            var isSuccess = true
            FileUtils.readFileChunks(srcFileSimpleInfoPath, MAX_LENGTH.toLong()) {
                if (it.isSuccess) {
                    val result = it.getOrNull() ?: Pair(0L, byteArrayOf())
                    mainScope.launch {
                        val resultWrite = rpc.fileService.writeBytes(
                            rpc.token,
                            fileSize = fileSimpleInfo.size,
                            blockIndex = result.first,
                            blockLength = length,
                            path = destFileSimpleInfoPath,
                            byteArray = result.second,
                        )
                        if (!resultWrite.isSuccess) {
                            isSuccess = false
                            // TODO 记录错误
                        }
                        length--
                    }
                } else {
                    isSuccess = false
                    // TODO 记录错误
                    length--
                    replyCallback(Result.failure(it.exceptionOrNull() ?: Exception()))
                }
            }

            while (length > 0 || isSuccess) {
                delay(300)
            }

            replyCallback(Result.success(length == 0L && isSuccess))
            return
        }

        // 远程 to 本地
        if (srcFileSimpleInfo.protocol == FileProtocol.Device && destFileSimpleInfo.protocol == FileProtocol.Local) {
            if (fileSimpleInfo.size == 0L) {
                FileUtils.createFile(destFileSimpleInfoPath)
                    .onSuccess { success -> replyCallback(Result.success(success)) }
                    .onFailure { failure -> replyCallback(Result.failure(failure)) }
                return
            }

            var isSuccess = true
            streamScoped {
                rpc.fileService.readFileChunks(
                    rpc.token,
                    srcFileSimpleInfoPath,
                    MAX_LENGTH.toLong()
                ).collect { result ->
                    if (result.isSuccess && result.value != null) {
                        val writeBytes = FileUtils.writeBytes(
                            path = destFileSimpleInfoPath,
                            fileSize = fileSimpleInfo.size,
                            data = result.value.second,
                            offset = result.value.first * MAX_LENGTH
                        )
                        if (writeBytes.isFailure) {
                            isSuccess = false
                        }
                    } else {
                        isSuccess = false
                    }
                }
            }
            replyCallback(Result.success(isSuccess))

            return
        }


        // 远程 to 远程
        if (srcFileSimpleInfo.protocol == FileProtocol.Device && destFileSimpleInfo.protocol == FileProtocol.Device) {
            var destFileService: FileService = rpc.fileService
            if (srcFileSimpleInfo.protocol == FileProtocol.Device && destFileSimpleInfo.protocol == FileProtocol.Device) {
                val socketDevice =
                    deviceState.socketDevices.firstOrNull { it.id == destFileSimpleInfo.protocolId && it.client != null }
                if (socketDevice == null) {
                    replyCallback(Result.failure(Exception("设备离线")))
                    return
                }
                destFileService = socketDevice.client!!.fileService
            }
            if (fileSimpleInfo.size == 0L) {
                val createFile = destFileService.createFile(
                    rpc.token,
                    listOf(destFileSimpleInfoPath)
                )
                if (!createFile.isSuccess) {
                    if (createFile.value?.first()?.isSuccess == false) {
                        replyCallback(Result.failure(createFile.deSerializable()))
                        return
                    }
                }
                replyCallback(Result.success(createFile.value?.first()?.value == true))
                return
            }


            var isSuccess = true
            streamScoped {
                rpc.fileService.readFileChunks(
                    rpc.token,
                    srcFileSimpleInfoPath,
                    MAX_LENGTH.toLong()
                ).collect { result ->
                    if (result.isSuccess && result.value != null) {
                        val writeBytes = destFileService.writeBytes(
                            token = rpc.token,
                            fileSize = fileSimpleInfo.size,
                            blockIndex = result.value.first,
                            blockLength = length,
                            path = destFileSimpleInfoPath,
                            byteArray = result.value.second
                        )
                        if (!writeBytes.isSuccess) {
                            isSuccess = false
                        }
                    } else {
                        isSuccess = false
                    }
                }
            }
            replyCallback(Result.success(isSuccess))

            return
        }


        replyCallback(Result.success(false))
    }

    suspend fun getFile(id: String, path: String, replyCallback: (Result<FileSimpleInfo>) -> Unit) {
        val result = rpc.fileService.getFileByPath(rpc.token, path)
        if (!result.isSuccess) {
            replyCallback(Result.failure(result.deSerializable()))
            return
        }

        if (result.value == null) {
            replyCallback(Result.failure(EmptyDataException()))
            return
        }

        replyCallback(Result.success(result.value))
    }

    suspend fun getFile(id: String, path: String, name: String, replyCallback: (Result<FileSimpleInfo>) -> Unit) {
        val result = rpc.fileService.getFileByPathAndName(rpc.token, path, name)
        if (!result.isSuccess) {
            replyCallback(Result.failure(result.deSerializable()))
            return
        }

        if (result.value == null) {
            replyCallback(Result.failure(EmptyDataException()))
            return
        }

        replyCallback(Result.success(result.value))
    }
}