package app.filemanager.service.handle

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.FileSizeInfo
import app.filemanager.service.BaseSocketManager.Companion.MAX_LENGTH
import app.filemanager.service.SocketClientManger
import app.filemanager.service.WebSocketResult
import app.filemanager.service.socket.SocketHeader
import app.filemanager.utils.FileUtils
import kotlinx.coroutines.MainScope
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

        socket.waitFinish(replyKey, callback = {
            val temp = socket.replyMessage[replyKey] as ByteArray
            val webSocketResult = ProtoBuf.decodeFromByteArray<WebSocketResult<Boolean>>(temp)
            if (webSocketResult.isSuccess) {
                replyCallback(Result.success(webSocketResult.value ?: false))
            } else {
                replyCallback(Result.failure(webSocketResult.deSerializable()))
            }

            socket.replyMessage.remove(replyKey)
            return@waitFinish true
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
            params = mapOf(
                "replyKey" to replyKey.toString(),
                "path" to path,
                "name" to name
            ),
            value = ""
        )
        socket.waitFinish(replyKey, callback = {
            val decodeFromHexString =
                ProtoBuf.decodeFromByteArray<WebSocketResult<Boolean>>(
                    socket.replyMessage[replyKey] as ByteArray
                )

            if (decodeFromHexString.isSuccess) {
                replyCallback(Result.success(decodeFromHexString.value ?: false))
            } else {
                replyCallback(Result.failure(decodeFromHexString.deSerializable()))
            }

            socket.replyMessage.remove(replyKey)
            return@waitFinish true
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
        socket.waitFinish(replyKey, callback = {
            val decodeFromHexString =
                ProtoBuf.decodeFromByteArray<WebSocketResult<FileSizeInfo>>(
                    socket.replyMessage[replyKey] as ByteArray
                )

            if (decodeFromHexString.isSuccess) {
                replyCallback(Result.success(decodeFromHexString.value as FileSizeInfo))
            } else {
                replyCallback(Result.failure(decodeFromHexString.deSerializable()))
            }

            socket.replyMessage.remove(replyKey)
            return@waitFinish true
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

        socket.waitFinish(replyKey, callback = {
            val decodeFromHexString =
                ProtoBuf.decodeFromByteArray<WebSocketResult<List<Boolean>>>(
                    socket.replyMessage[replyKey] as ByteArray
                )

            if (decodeFromHexString.isSuccess) {
                replyCallback(Result.success(decodeFromHexString.value as List<Boolean>))
            } else {
                replyCallback(Result.failure(decodeFromHexString.deSerializable()))
            }

            socket.replyMessage.remove(replyKey)
            return@waitFinish true
        })
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun writeBytes(id: String, srcPath: String, destPath: String, replyCallback: (Result<Boolean>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        val file = FileUtils.getFile(srcPath)
        val length = ceil(file.size / MAX_LENGTH.toFloat()).toInt()

        FileUtils.readFileChunks(srcPath, MAX_LENGTH.toLong()) {
            if (it.isSuccess) {
                val result = it.getOrNull() ?: Pair(0, byteArrayOf())
                MainScope().launch {
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
            }
        }


        socket.waitFinish(replyKey, callback = {
            val decodeFromHexString = socket.replyMessage[replyKey] as MutableList<Long>

            println("写入失败的块 = ${decodeFromHexString}")

            if (decodeFromHexString.isEmpty()) {
                replyCallback(Result.success(true))
            } else {
                replyCallback(Result.failure(Exception()))
            }

            socket.replyMessage.remove(replyKey)
            return@waitFinish true
        })
    }
}