package app.filemanager.service.handle

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.FileSizeInfo
import app.filemanager.exception.EmptyDataException
import app.filemanager.service.data.RenameInfo
import app.filemanager.service.rpc.FileService
import app.filemanager.service.rpc.RpcClientManager.Companion.MAX_LENGTH
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.streamScoped
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.ceil

class FileHandle(private val fileService: FileService) : KoinComponent {
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
        val result = fileService.rename(
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
        val result = fileService.createFolder(listOf("$path${PathUtils.getPathSeparator()}$name"))
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
        val result = fileService.getSizeInfo(totalSpace, freeSpace, fileSimpleInfo)
        if (!result.isSuccess) {
            replyCallback(Result.failure(result.deSerializable()))
            return
        }
        replyCallback(Result.success(result.value as FileSizeInfo))
    }

    suspend fun deleteFile(id: String, paths: List<String>, replyCallback: (Result<List<Boolean>>) -> Unit) {
        val result = fileService.delete(paths)

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

        val length = ceil(fileSimpleInfo.size / MAX_LENGTH.toFloat()).toLong()
        val mainScope = MainScope()

        // 本地 to 远程
        if (srcFileSimpleInfo.protocol == FileProtocol.Local && destFileSimpleInfo.protocol == FileProtocol.Device) {
            if (fileSimpleInfo.size == 0L) {
                val createFile = fileService.createFile(listOf(destFileSimpleInfoPath))
                if (createFile.isSuccess) {
                    replyCallback(Result.success(createFile.value?.first()?.value == true))
                } else {
                    replyCallback(Result.failure(createFile.deSerializable()))
                }
                return
            }
            FileUtils.readFileChunks(srcFileSimpleInfoPath, MAX_LENGTH.toLong()) {
                if (it.isSuccess) {
                    val result = it.getOrNull() ?: Pair(0L, byteArrayOf())
                    mainScope.launch {
                        val resultWrite = fileService.writeBytes(
                            fileSize = fileSimpleInfo.size,
                            blockIndex = result.first,
                            blockLength = length,
                            path = destFileSimpleInfoPath,
                            byteArray = result.second,
                        )
                        replyCallback(Result.success(resultWrite.isSuccess))
                    }
                } else {
                    replyCallback(Result.failure(it.exceptionOrNull() ?: Exception()))
                }
            }

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
                fileService.readFileChunks(srcFileSimpleInfoPath, MAX_LENGTH.toLong()).collect { result ->
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
            var destFileService: FileService = fileService
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
                val createFile = destFileService.createFile(listOf(destFileSimpleInfoPath))
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
                fileService.readFileChunks(srcFileSimpleInfoPath, MAX_LENGTH.toLong()).collect { result ->
                    if (result.isSuccess && result.value != null) {
                        val writeBytes = destFileService.writeBytes(
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
        val result = fileService.getFileByPath(path)
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
        val result = fileService.getFileByPathAndName(path, name)
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