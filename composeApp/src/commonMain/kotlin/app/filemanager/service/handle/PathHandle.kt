package app.filemanager.service.handle

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.PathInfo
import app.filemanager.extensions.pathLevel
import app.filemanager.service.rpc.RpcClientManager
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.streamScoped
import kotlinx.serialization.ExperimentalSerializationApi


class PathHandle(private val rpc: RpcClientManager) {
    /**
     * 从远程设备获取指定路径下的文件和文件夹列表。
     *
     * @param path 要获取列表的路径。
     * @param remoteId 远程设备的ID。
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getList(path: String, remoteId: String, replyCallback: (Result<List<FileSimpleInfo>>) -> Unit) {
        val fileSimpleInfos: MutableList<FileSimpleInfo> = mutableListOf()

        val result = rpc.pathService.list(path)
        if (!result.isSuccess) {
            replyCallback(Result.failure(result.deSerializable()))
            return
        }

        result.value?.forEach {
            it.value.forEach { fileSimpleInfo ->
                fileSimpleInfos.add(fileSimpleInfo.apply {
                    protocol = it.key.first
                    protocolId = it.key.second
                    this.path = path + this.path
                })
            }
        }
        replyCallback(Result.success(fileSimpleInfos))
    }

    suspend fun getRootPaths(remoteId: String, replyCallback: (List<PathInfo>) -> Unit) {
        val result = rpc.pathService.rootPaths()
        if (!result.isSuccess) {
            replyCallback(listOf())
        }
        replyCallback(result.value.orEmpty())
    }

    suspend fun getTraversePath(path: String, remoteId: String, replyCallback: (Result<List<FileSimpleInfo>>) -> Unit) {
        streamScoped {
            rpc.pathService.traversePath(path).collect { result ->
                if (!result.isSuccess) {
                    replyCallback(Result.failure(result.deSerializable()))
                } else {
                    val fileSimpleInfos: MutableList<FileSimpleInfo> = mutableListOf()
                    result.value?.forEach {
                        it.value.forEach { fileSimpleInfo ->
                            fileSimpleInfos.add(fileSimpleInfo.apply {
                                protocol = it.key.first
                                protocolId = it.key.second
                                this.path = path + this.path
                            })
                        }
                    }

                    replyCallback(Result.success(fileSimpleInfos))
                }
            }
        }
    }

    // TODO 遍历目录->创建文件夹->创建文件
    // [y] TODO 2.本地复制到远程
    // TODO 3.远程复制到本地
    // TODO 4.远程复制到远程
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun copyFile(
        remoteId: String,
        srcPath: String,
        destPath: String,
        replyCallback: (Result<Boolean>) -> Unit
    ) {
        val mainScope = MainScope()
        var successCount = 0
        var failureCount = 0

        // TODO 2.本地复制到远程
        val list = mutableListOf<FileSimpleInfo>()
        PathUtils.traverse(srcPath) { fileAndFolder ->
            if (fileAndFolder.isSuccess) {
                list.addAll(fileAndFolder.getOrNull() ?: listOf())
            }
        }
        list.add(FileUtils.getFile(srcPath))

        list.sortedWith(
            compareBy<FileSimpleInfo> { !it.isDirectory }
                .thenBy { it.path.pathLevel() })
            .groupBy { it.isDirectory }.forEach { (isDir, fileSimpleInfos) ->
                if (isDir) {
                    for (paths in fileSimpleInfos.map { it.path.replaceFirst(srcPath, destPath) }.chunked(30)) {
                        mainScope.launch {
                            val result = rpc.fileService.createFolder(paths)
                            if (result.isSuccess) {
                                result.value.orEmpty().forEach { item ->
                                    when {
                                        item.isSuccess && item.value == true -> successCount++
                                        item.isSuccess -> failureCount++
                                        else -> failureCount++ // TODO 记录文件夹创建错误
                                    }
                                }
                            } else {
                                failureCount += paths.size
                            }
                        }
                    }

                    while (successCount + failureCount < fileSimpleInfos.size) {
                        delay(100L)
                    }
                } else {
                    for (files in fileSimpleInfos.chunked(maxOf(30, fileSimpleInfos.size / 30))) {
                        mainScope.launch {
                            for (file in files) {
                                rpc.fileHandle.writeBytes(
                                    remoteId,
                                    file.path,
                                    file.path.replaceFirst(srcPath, destPath)
                                ) {
                                    if (it.getOrNull() == true) {
                                        successCount++
                                    } else {
                                        println(file.path)
                                        failureCount++
                                    }
                                }
                            }
                        }
                    }
                }
            }

        while (successCount + failureCount < list.size) {
            delay(100L)
        }

        replyCallback(Result.success(successCount + failureCount == list.size))
    }
}