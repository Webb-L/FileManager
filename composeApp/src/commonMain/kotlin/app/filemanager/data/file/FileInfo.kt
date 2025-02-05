package app.filemanager.data.file

import app.filemanager.extensions.pathLevel
import app.filemanager.service.rpc.RpcClientManager.Companion.MAX_LENGTH
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.math.ceil

@Serializable
data class FileSimpleInfo(
    val name: String,
    val description: String = "",
    val isDirectory: Boolean,
    val isHidden: Boolean,
    var path: String,
    val mineType: String,
    val size: Long,
    val createdDate: Long,
    val updatedDate: Long,
    var protocol: FileProtocol = FileProtocol.Local,
    var protocolId: String = "",
) {
    companion object {
        fun nullFileSimpleInfo() = FileSimpleInfo(
            name = "",
            description = "",
            isDirectory = false,
            isHidden = false,
            path = "",
            mineType = "",
            size = 0,
            createdDate = 0,
            updatedDate = 0
        )

        fun pathFileSimpleInfo(path: String) = FileSimpleInfo(
            name = path,
            description = "",
            isDirectory = true,
            isHidden = false,
            path = path,
            mineType = "",
            size = 0,
            createdDate = 0,
            updatedDate = 0
        )
    }

    fun toFileInfo(
        permissions: Int,
        user: String,
        userGroup: String,
    ): FileInfo = FileInfo(
        name = name,
        description = description,
        isDirectory = isDirectory,
        isHidden = isHidden,
        path = path,
        mineType = mineType,
        size = size,
        permissions = permissions,
        user = user,
        userGroup = userGroup,
        createdDate = createdDate,
        updatedDate = updatedDate,
        protocol = protocol,
        protocolId = protocolId,
    )

    fun getSizeInfo(totalSpace: Long, freeSpace: Long): FileSizeInfo {
        var fileCount = 0L
        var folderCount = 0L
        var fileSize = -1L
        if (isDirectory) {
            PathUtils.traverse(path) {
                if (it.isSuccess) {
                    fileSize += it.getOrNull()?.sumOf { fileSimpleInfo ->
                        if (fileSimpleInfo.isDirectory) {
                            folderCount++
                            0
                        } else {
                            fileCount++
                            fileSimpleInfo.size
                        }
                    } ?: 0
                }
            }
        } else {
            fileSize = size
        }
        return FileSizeInfo(
            fileSize = fileSize,
            fileCount = fileCount,
            folderCount = folderCount,
            totalSpace = totalSpace,
            freeSpace = freeSpace
        )
    }

    fun withCopy(
        name: String? = null,
        description: String? = null,
        isDirectory: Boolean? = null,
        isHidden: Boolean? = null,
        path: String? = null,
        mineType: String? = null,
        size: Long? = null,
        createdDate: Long? = null,
        updatedDate: Long? = null,
        protocol: FileProtocol? = null,
        protocolId: String? = null,
    ): FileSimpleInfo {
        return FileSimpleInfo(
            name = name ?: this.name,
            description = description ?: this.description,
            isDirectory = isDirectory ?: this.isDirectory,
            isHidden = isHidden ?: this.isHidden,
            path = path ?: this.path,
            mineType = mineType ?: this.mineType,
            size = size ?: this.size,
            createdDate = createdDate ?: this.createdDate,
            updatedDate = updatedDate ?: this.updatedDate,
            protocol = protocol ?: this.protocol,
            protocolId = protocolId ?: this.protocolId,
        )
    }

    fun writeToFile(destPath: String): Result<Boolean> {
        if (size == 0L) {
            return FileUtils.createFile(destPath)
        }

        var length = ceil(size / MAX_LENGTH.toFloat()).toInt()
        var isSuccess = true
        FileUtils.readFileChunks(path, MAX_LENGTH.toLong()) {
            if (it.isSuccess) {
                val result = it.getOrNull() ?: Pair(0, byteArrayOf())
                val writeResult = FileUtils.writeBytes(
                    destPath,
                    size,
                    result.second,
                    result.first.toLong() * MAX_LENGTH
                )
                length--
                if (writeResult.isFailure) {
                    isSuccess = false
                }
            } else {
                isSuccess = false
                length--
            }
        }
        return Result.success(isSuccess && length <= 0)
    }

    suspend fun copyToFile(destPath: String): Result<Boolean> {
        val mainScope = MainScope()
        var successCount = 0
        var failureCount = 0

        // 复制文件
        if (!isDirectory) {
            val targetFilePath = path.replaceFirst(path, destPath)
            writeToFile(targetFilePath)
                .onSuccess { successCount++ }
                .onFailure { failureCount++ }

            return Result.success(successCount == 1 && failureCount == 0)
        }


        // 复制文件夹
        val list = mutableListOf<FileSimpleInfo>()
        PathUtils.traverse(path) { fileAndFolder ->
            if (fileAndFolder.isSuccess) {
                list.addAll(fileAndFolder.getOrDefault(listOf()))
            }
        }

        val rootFolder = FileUtils.createFolder(destPath)
        if (rootFolder.isFailure) {
            return rootFolder
        }

        list.sortedWith(
            compareBy<FileSimpleInfo> { !it.isDirectory }
                .thenBy { it.path.pathLevel() })
            .groupBy { it.isDirectory }.forEach { (isDir, fileSimpleInfos) ->
                if (isDir) {
                    for (paths in fileSimpleInfos.map { it.path.replaceFirst(path, destPath) }
                        .chunked(30)) {
                        mainScope.launch {
                            val count = paths.count {
                                FileUtils.createFolder(it).getOrDefault(false)
                            }

                            successCount += count
                            failureCount += paths.size - count
                        }
                    }

                    while (successCount + failureCount < fileSimpleInfos.size) {
                        delay(100L)
                    }
                } else {
                    for (files in fileSimpleInfos.chunked(maxOf(30, fileSimpleInfos.size / 30))) {
                        mainScope.launch {
                            for (file in files) {
                                file.writeToFile(
                                    file.path.replaceFirst(
                                        path,
                                        destPath
                                    )
                                )
                                    .onSuccess { successCount++ }
                                    .onFailure { failureCount++ }
                            }
                        }
                    }
                }
            }

        while (successCount + failureCount < list.size) {
            delay(100L)
        }


        val isFinish = list.size == successCount && failureCount == 0
//            if (isFinish) {
//                taskState.tasks.remove(task)
//            }
        return Result.success(isFinish)
    }
}

@Serializable
data class FileInfo(
    val name: String,
    val description: String = "",
    val isDirectory: Boolean,
    val isHidden: Boolean,
    val path: String,
    val mineType: String,
    val size: Long,
    val permissions: Int,
    val user: String,
    val userGroup: String,
    val createdDate: Long,
    val updatedDate: Long,
    var protocol: FileProtocol = FileProtocol.Local,
    var protocolId: String = "",
) {
    companion object {
        fun nullFileInfo() = FileInfo(
            name = "",
            description = "",
            isDirectory = false,
            isHidden = false,
            path = "",
            mineType = "",
            size = 0,
            permissions = 0,
            user = "",
            userGroup = "",
            createdDate = 0,
            updatedDate = 0
        )

        fun pathFileInfo(path: String) = FileInfo(
            name = path,
            description = "",
            isDirectory = true,
            isHidden = false,
            path = path,
            mineType = "",
            size = 0,
            permissions = 0,
            user = "",
            userGroup = "",
            createdDate = 0,
            updatedDate = 0
        )
    }
}

@Serializable
data class FileSizeInfo(
    val fileSize: Long = 0,
    val fileCount: Long = 0,
    val folderCount: Long = 0,
    val totalSpace: Long = 0,
    val freeSpace: Long = 0,
)