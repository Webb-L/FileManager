package app.filemanager.service.handle

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.FileSizeInfo
import app.filemanager.exception.EmptyDataException
import app.filemanager.service.data.RenameInfo
import app.filemanager.service.rpc.FileService
import app.filemanager.service.rpc.RpcClientManager.Companion.MAX_LENGTH
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil

class FileHandle(private val fileService: FileService) {
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

    suspend fun writeBytes(id: String, srcPath: String, destPath: String, replyCallback: (Result<Boolean>) -> Unit) {
        val file = fileService.getFileByPath(srcPath).value
        if (file == null) {
            replyCallback(Result.failure(EmptyDataException()))
            return
        }
        if (!file.isDirectory) {
            val result = fileService.createFile(destPath)
            if (!result.isSuccess) {
                replyCallback(Result.failure(result.deSerializable()))
            } else {
                replyCallback(Result.success(result.value ?: false))
            }
            return
        }

        val length = ceil(file.size / MAX_LENGTH.toFloat()).toLong()
        val mainScope = MainScope()

        var successCount = 0L
        var failureCount = 0L

        FileUtils.readFileChunks(srcPath, MAX_LENGTH.toLong()) {
            if (it.isSuccess) {
                val result = it.getOrNull() ?: Pair(0L, byteArrayOf())
                mainScope.launch {
                    val resultWrite = fileService.writeBytes(
                        fileSize = file.size,
                        blockIndex = result.first,
                        blockLength = length,
                        path = destPath,
                        byteArray = result.second,
                    )
                    if (resultWrite.isSuccess) {
                        successCount++
                    } else {
                        failureCount++
                    }
                }
            } else {
                replyCallback(Result.failure(it.exceptionOrNull() ?: Exception()))
                failureCount++
            }
        }


        while (successCount + failureCount < length) {
            delay(100L)
        }

        replyCallback(Result.success(successCount == length && failureCount == 0L))
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