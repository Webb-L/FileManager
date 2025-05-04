package app.filemanager.service.handle

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.PathInfo
import app.filemanager.extensions.pathLevel
import app.filemanager.service.rpc.FileService
import app.filemanager.service.rpc.RpcClientManager
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.streamScoped
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PathHandle(private val rpc: RpcClientManager) : KoinComponent {
    val fileState: FileState by inject()
    val deviceState: DeviceState by inject()

    /**
     * 从远程设备获取指定路径下的文件和文件夹列表。
     *
     * @param path 要获取列表的路径。
     * @param remoteId 远程设备的ID。
     */
    suspend fun getList(path: String, remoteId: String, replyCallback: (Result<List<FileSimpleInfo>>) -> Unit) {
        val fileSimpleInfos: MutableList<FileSimpleInfo> = mutableListOf()

        val result = rpc.pathService.list(rpc.token, path)
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
        val result = rpc.pathService.rootPaths(rpc.token)
        if (!result.isSuccess) {
            replyCallback(listOf())
        }
        replyCallback(result.value.orEmpty())
    }

    suspend fun getTraversePath(path: String, remoteId: String, replyCallback: (Result<List<FileSimpleInfo>>) -> Unit) {
        streamScoped {
            rpc.pathService.traversePath(rpc.token, path).collect { result ->
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

    /**
     * 复制文件或文件夹到目标路径。
     *
     * @param remoteId 远程设备的标识。
     * @param srcFileSimpleInfo 源文件或文件夹的信息。
     * @param destFileSimpleInfo 目标文件或文件夹的信息。
     * @param replyCallback 回调操作结果，返回一个包含布尔值的结果对象，表示操作是否成功。
     */
    suspend fun copyFile(
        remoteId: String,
        srcFileSimpleInfo: FileSimpleInfo,
        destFileSimpleInfo: FileSimpleInfo,
        replyCallback: (Result<Boolean>) -> Unit
    ) {
        println("copyFile: $srcFileSimpleInfo -> $destFileSimpleInfo")
        val mainScope = MainScope()
        var successCount = 0
        var failureCount = 0

        // 只复制一个文件
        if (!srcFileSimpleInfo.isDirectory) {
            rpc.fileHandle.writeBytes(
                remoteId,
                srcFileSimpleInfo,
                destFileSimpleInfo,
                srcFileSimpleInfo
            ) {
                replyCallback(it)
            }
            return
        }

        val list = mutableListOf<FileSimpleInfo>()


        // 获取本地所有的文件和文件夹
        if (srcFileSimpleInfo.protocol == FileProtocol.Local) {
            // 只有一个文件夹或文件
            if (srcFileSimpleInfo.size == 0L) {
                val result = if (srcFileSimpleInfo.isDirectory)
                    FileUtils.createFolder(destFileSimpleInfo.path)
                else
                    FileUtils.createFile(destFileSimpleInfo.path)

                result
                    .onSuccess { success -> replyCallback(Result.success(success)) }
                    .onFailure { failure -> replyCallback(Result.failure(failure)) }
                return
            }

            // 获取所有文件和文件夹
            PathUtils.traverse(srcFileSimpleInfo.path) { fileAndFolder ->
                if (fileAndFolder.isSuccess) {
                    list.addAll(fileAndFolder.getOrDefault(listOf()).map {
                        it.path = it.path.replaceFirst(srcFileSimpleInfo.path, "")
                        it
                    })
                }
            }
        }

        // 获取远程所有的文件和文件夹
        if (srcFileSimpleInfo.protocol == FileProtocol.Device) {
            var fileService: FileService = rpc.fileService
            if (destFileSimpleInfo.protocol == FileProtocol.Device) {
                val socketDevice =
                    deviceState.socketDevices.firstOrNull { it.id == destFileSimpleInfo.protocolId && it.client != null }
                if (socketDevice == null) {
                    replyCallback(Result.failure(Exception("设备离线")))
                    return
                }
                fileService = socketDevice.client!!.fileService
            }

            // 只复制文件或文件夹
            if (srcFileSimpleInfo.size == 0L) {
                val result = if (srcFileSimpleInfo.isDirectory)
                    fileService.createFolder(rpc.token,listOf(destFileSimpleInfo.path))
                else
                    fileService.createFile(rpc.token,listOf(destFileSimpleInfo.path))

                if (result.isSuccess) {
                    replyCallback(Result.success(result.value?.first()?.value == true))
                } else {
                    replyCallback(Result.failure(result.deSerializable()))
                }
                return
            }

            streamScoped {
                rpc.pathService.traversePath(rpc.token,srcFileSimpleInfo.path).collect { fileAndFolder ->
                    if (fileAndFolder.isSuccess) {
                        fileAndFolder.value?.forEach {
                            list.addAll(it.value)
                        }
                    }
                }
            }
        }

        if (destFileSimpleInfo.isDirectory) {
            if (destFileSimpleInfo.protocol == FileProtocol.Local) {
                val createFolder = FileUtils.createFolder(destFileSimpleInfo.path)
                if (createFolder.isFailure) {
                    replyCallback(Result.failure(createFolder.exceptionOrNull() ?: Exception()))
                    return
                }
            }

            if (destFileSimpleInfo.protocol == FileProtocol.Device) {
                var fileService: FileService = rpc.fileService
                if (srcFileSimpleInfo.protocol == FileProtocol.Device) {
                    val socketDevice =
                        deviceState.socketDevices.firstOrNull { it.id == destFileSimpleInfo.protocolId && it.client != null }
                    if (socketDevice == null) {
                        replyCallback(Result.failure(Exception("设备离线")))
                        return
                    }
                    fileService = socketDevice.client!!.fileService
                }
                val createFolder = fileService.createFolder(rpc.token,listOf(destFileSimpleInfo.path))
                if (!createFolder.isSuccess) {
                    replyCallback(Result.failure(createFolder.deSerializable()))
                    return
                } else {
                    if (createFolder.value?.first()?.isSuccess == false) {
                        replyCallback(Result.failure(createFolder.deSerializable()))
                        return
                    }
                }
            }
        }

        list.sortedWith(
            compareBy<FileSimpleInfo> { !it.isDirectory }
                .thenBy { it.path.pathLevel() })
            .groupBy { it.isDirectory }.forEach { (isDir, fileSimpleInfos) ->
                if (isDir) {
                    var fileService: FileService = rpc.fileService
                    if (srcFileSimpleInfo.protocol == FileProtocol.Device && destFileSimpleInfo.protocol == FileProtocol.Device) {
                        val socketDevice =
                            deviceState.socketDevices.firstOrNull { it.id == destFileSimpleInfo.protocolId && it.client != null }
                        if (socketDevice == null) {
                            replyCallback(Result.failure(Exception("设备离线")))
                            return
                        }
                        fileService = socketDevice.client!!.fileService
                    }

                    for (paths in fileSimpleInfos.map { "${destFileSimpleInfo.path}${it.path}" }.chunked(30)) {
                        if (destFileSimpleInfo.protocol == FileProtocol.Local) {
                            paths.forEach {
                                FileUtils.createFolder(it)
                                    .onSuccess { success -> if (success) successCount++ else failureCount++ }
                                    .onFailure { failureCount++ }
                            }
                        }

                        if (destFileSimpleInfo.protocol == FileProtocol.Device) {
                            val result = fileService.createFolder(rpc.token,paths)
                            if (result.isSuccess) {
                                result.value.orEmpty().forEach { item ->
                                    when {
                                        item.isSuccess && item.value == true -> successCount++
                                        else -> failureCount++ // TODO 记录文件夹创建错误
                                    }
                                }
                            } else {
                                failureCount += paths.size
                            }
                        }
                    }
                } else {
                    for (files in fileSimpleInfos.chunked(maxOf(30, fileSimpleInfos.size / 30))) {
                        mainScope.launch(Dispatchers.Default) {
                            for (file in files) {
                                rpc.fileHandle.writeBytes(
                                    remoteId,
                                    srcFileSimpleInfo,
                                    destFileSimpleInfo,
                                    file
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