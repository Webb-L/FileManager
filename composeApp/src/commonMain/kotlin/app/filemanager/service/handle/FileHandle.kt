package app.filemanager.service.handle

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.FileSizeInfo
import app.filemanager.service.BaseSocketManager.Companion.MAX_LENGTH
import app.filemanager.service.SocketClientManger
import app.filemanager.service.WebSocketResult
import app.filemanager.service.socket.SocketHeader
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.math.ceil
import kotlin.random.Random

class FileHandle(private val socket: SocketClientManger) {
    /**
     * 重命名文件和文件夹
     *
     * @param remoteId
     * @param path
     * @param oldName
     * @param newName
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun rename(
        remoteId: String,
        path: String,
        oldName: String,
        newName: String,
        replyCallback: (Result<Boolean>) -> Unit
    ) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        socket.send(
            header = SocketHeader(command = "rename"),
            params = mapOf(
                "replyKey" to replyKey.toString(),
                "path" to path,
                "oldName" to oldName,
                "newName" to newName
            ),
            value = ""
        )

        socket.waitFinish(replyKey, callback = { result ->
            if (result.isFailure) {
                replyCallback(Result.failure(result.exceptionOrNull() ?: Exception()))
                return@waitFinish
            }

            val webSocketResult =
                ProtoBuf.decodeFromByteArray<WebSocketResult<Boolean>>(result.getOrDefault(byteArrayOf()))
            if (webSocketResult.isSuccess) {
                replyCallback(Result.success(webSocketResult.value ?: false))
            } else {
                replyCallback(Result.failure(webSocketResult.deSerializable()))
            }
        })
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
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun createFolder(remoteId: String, path: String, name: String, replyCallback: (Result<Boolean>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        socket.send(
            header = SocketHeader(command = "createFolder"),
            params = mapOf("replyKey" to replyKey.toString()),
            value = listOf("$path${PathUtils.getPathSeparator()}$name")
        )
        socket.waitFinish(replyKey, callback = { result ->
            if (result.isFailure) {
                replyCallback(Result.failure(result.exceptionOrNull() ?: Exception()))
                return@waitFinish
            }

            val resultList = ProtoBuf.decodeFromByteArray<List<WebSocketResult<Boolean>>>(
                result.getOrDefault(byteArrayOf())
            )
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
        })
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getFileSizeInfo(
        id: String,
        fileSimpleInfo: FileSimpleInfo,
        totalSpace: Long,
        freeSpace: Long,
        replyCallback: (Result<FileSizeInfo>) -> Unit
    ) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        socket.send(
            header = SocketHeader(command = "getSizeInfo"),
            params = mapOf(
                "replyKey" to replyKey.toString(),
                "totalSpace" to totalSpace.toString(),
                "freeSpace" to freeSpace.toString()
            ),
            value = fileSimpleInfo
        )
        socket.waitFinish(replyKey, callback = { result ->
            if (result.isFailure) {
                replyCallback(Result.failure(result.exceptionOrNull() ?: Exception()))
                return@waitFinish
            }

            val decodeFromHexString =
                ProtoBuf.decodeFromByteArray<WebSocketResult<FileSizeInfo>>(
                    result.getOrDefault(byteArrayOf())
                )
            if (decodeFromHexString.isSuccess) {
                replyCallback(Result.success(decodeFromHexString.value as FileSizeInfo))
            } else {
                replyCallback(Result.failure(decodeFromHexString.deSerializable()))
            }
        })
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun deleteFile(id: String, paths: List<String>, replyCallback: (Result<List<Boolean>>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        socket.send(
            header = SocketHeader(command = "deleteFile"),
            params = mapOf(
                "replyKey" to replyKey.toString(),
            ),
            value = paths
        )

        socket.waitFinish(replyKey, callback = { result ->
            if (result.isFailure) {
                replyCallback(Result.failure(result.exceptionOrNull() ?: Exception()))
                return@waitFinish
            }

            val decodeFromHexString =
                ProtoBuf.decodeFromByteArray<WebSocketResult<List<Boolean>>>(
                    result.getOrDefault(byteArrayOf())
                )
            if (decodeFromHexString.isSuccess) {
                replyCallback(Result.success(decodeFromHexString.value as List<Boolean>))
            } else {
                replyCallback(Result.failure(decodeFromHexString.deSerializable()))
            }
        })
    }

    suspend fun writeBytes(id: String, srcPath: String, destPath: String, replyCallback: (Result<Boolean>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        val file = FileUtils.getFile(srcPath)
        val length = ceil(file.size / MAX_LENGTH.toFloat()).toInt()
        val mainScope = MainScope()
        FileUtils.readFileChunks(srcPath, MAX_LENGTH.toLong()) {
            if (it.isSuccess) {
                val result = it.getOrNull() ?: Pair(0, byteArrayOf())
                mainScope.launch {
                    if (socket.cancelKeys.contains(replyKey)) {
                        mainScope.cancel()
                        return@launch
                    }
                    socket.send(
                        header = SocketHeader(command = "writeBytes"),
                        params = mapOf(
                            "replyKey" to replyKey.toString(),
                            "fileSize" to file.size.toString(),
                            "blockIndex" to result.first.toString(),
                            "blockLength" to length.toString(),
                            "path" to destPath,
                        ),
                        value = result.second
                    )
                }
            } else {
                replyCallback(Result.failure(it.exceptionOrNull() ?: Exception()))
            }
        }


        socket.waitFinish<List<Long>>(replyKey, callback = { result ->
            if (result.isFailure) {
                replyCallback(Result.failure(result.exceptionOrNull() ?: Exception()))
                return@waitFinish
            }
            socket.cancelKeys.remove(replyKey)

            val decodeFromHexString = result.getOrDefault(listOf())

//            println("写入失败的块 = ${decodeFromHexString}")

            if (decodeFromHexString.isEmpty()) {
                replyCallback(Result.success(true))
            } else {
                replyCallback(Result.failure(Exception()))
            }
        })
    }
}