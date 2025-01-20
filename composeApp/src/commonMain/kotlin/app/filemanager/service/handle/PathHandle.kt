package app.filemanager.service.handle

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.PathInfo
import app.filemanager.extensions.pathLevel
import app.filemanager.service.SocketClientManger
import app.filemanager.service.WebSocketResult
import app.filemanager.service.WebSocketResultMapListFileSimpleInfo
import app.filemanager.service.socket.SocketHeader
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.random.Random


class PathHandle(private val socket: SocketClientManger) {
    /**
     * 从远程设备获取指定路径下的文件和文件夹列表。
     *
     * @param path 要获取列表的路径。
     * @param remoteId 远程设备的ID。
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getList(path: String, remoteId: String, replyCallback: (Result<List<FileSimpleInfo>>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        socket.send(
            header = SocketHeader(command = "list"),
            params = mapOf("replyKey" to replyKey.toString(), "path" to path),
            value = ""
        )

        val fileSimpleInfos: MutableList<FileSimpleInfo> = mutableListOf()

        socket.waitFinish(replyKey, callback = { result ->
            if (result.isFailure) {
                replyCallback(Result.failure(result.exceptionOrNull() ?: Exception()))
                return@waitFinish
            }

            val decodeFromHexString =
                ProtoBuf.decodeFromByteArray<WebSocketResult<WebSocketResultMapListFileSimpleInfo>>(
                    result.getOrDefault(byteArrayOf())
                )

            if (decodeFromHexString.isSuccess) {
                (decodeFromHexString.value as Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>).forEach {
                    it.value.forEach { fileSimpleInfo ->
                        fileSimpleInfos.add(fileSimpleInfo.apply {
                            protocol = it.key.first
                            protocolId = it.key.second
                            this.path = path + this.path
                        })
                    }
                }
                replyCallback(Result.success(fileSimpleInfos))
            } else {
                replyCallback(Result.failure(decodeFromHexString.deSerializable()))
            }
        })
    }

    suspend fun getRootPaths(remoteId: String, replyCallback: (List<PathInfo>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        socket.send(
            header = SocketHeader(command = "rootPaths"),
            params = mapOf("replyKey" to replyKey.toString()),
            value = ""
        )


        // TODO 需要优化
        val paths: MutableList<PathInfo> = mutableListOf()
        socket.waitFinish(replyKey, callback = { result ->
            if (result.isFailure) {
                replyCallback(listOf())
                return@waitFinish
            }
            paths.addAll(result.getOrDefault(listOf()))
        })

        replyCallback(paths)
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getTraversePath(path: String, remoteId: String, replyCallback: (Result<List<FileSimpleInfo>>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        socket.send(
            header = SocketHeader(command = "traversePath"),
            params = mapOf("replyKey" to replyKey.toString(), "path" to path),
            value = ""
        )


        socket.waitFinish(replyKey, callback = { result ->
            if (result.isFailure) {
                replyCallback(Result.failure(result.exceptionOrNull() ?: Exception()))
                return@waitFinish
            }

            val decodeFromHexString =
                ProtoBuf.decodeFromByteArray<WebSocketResult<WebSocketResultMapListFileSimpleInfo>>(
                    result.getOrDefault(byteArrayOf())
                )

            if (decodeFromHexString.isSuccess) {
                val fileSimpleInfos: MutableList<FileSimpleInfo> = mutableListOf()
                (decodeFromHexString.value as WebSocketResultMapListFileSimpleInfo).forEach {
                    it.value.forEach { fileSimpleInfo ->
                        fileSimpleInfos.add(fileSimpleInfo.apply {
                            protocol = it.key.first
                            protocolId = it.key.second
                            this.path = path + this.path
                        })
                    }
                }
                replyCallback(Result.success(fileSimpleInfos))
            } else {
                replyCallback(Result.failure(decodeFromHexString.deSerializable()))
            }
        })
    }

    // TODO 遍历目录->创建文件夹->创建文件
    // TODO 1.本地复制
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

        list.sortedWith(
            compareBy<FileSimpleInfo> { !it.isDirectory }
                .thenBy { it.path.pathLevel() })
            .groupBy { it.isDirectory }.forEach { (isDir, fileSimpleInfos) ->
                if (isDir) {
                    for (paths in fileSimpleInfos.map { it.path.replaceFirst(srcPath, destPath) }.chunked(30)) {
                        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
                        mainScope.launch {
                            socket.send(
                                header = SocketHeader(command = "createFolder"),
                                params = mapOf(
                                    "replyKey" to replyKey.toString()
                                ),
                                value = paths
                            )
                            socket.waitFinish(replyKey, callback = { result ->
                                if (result.isFailure) {
                                    replyCallback(Result.failure(result.exceptionOrNull() ?: Exception()))
                                    return@waitFinish
                                }
                                socket.cancelKeys.remove(replyKey)

                                val webSocketResponse =
                                    ProtoBuf.decodeFromByteArray<WebSocketResult<List<WebSocketResult<Boolean>>>>(
                                        result.getOrDefault(byteArrayOf())
                                    )
                                if (webSocketResponse.isSuccess) {
                                    webSocketResponse.value.orEmpty().forEach { item ->
                                        when {
                                            item.isSuccess && item.value == true -> successCount++
                                            item.isSuccess -> failureCount++
                                            else -> failureCount++ // TODO 记录文件夹创建错误
                                        }
                                    }
                                } else {
                                    failureCount += paths.size
                                }
                            })
                        }
                    }

                    while (successCount + failureCount < fileSimpleInfos.size) {
                        delay(100L)
                    }
                } else {
                    for (files in fileSimpleInfos.chunked(fileSimpleInfos.size / 30)) {
                        mainScope.launch {
                            for (file in files) {
                                socket.fileHandle.writeBytes(
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