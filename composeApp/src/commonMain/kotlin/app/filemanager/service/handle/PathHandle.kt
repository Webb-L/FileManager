package app.filemanager.service.handle

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.PathInfo
import app.filemanager.service.SocketClientManger
import app.filemanager.service.WebSocketResult
import app.filemanager.service.WebSocketResultMapListFileSimpleInfo
import app.filemanager.service.socket.SocketHeader
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

        socket.waitFinish(replyKey, callback = {
            val decodeFromHexString =
                ProtoBuf.decodeFromByteArray<WebSocketResult<WebSocketResultMapListFileSimpleInfo>>(
                    socket.replyMessage[replyKey] as ByteArray
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

            socket.replyMessage.remove(replyKey)
            return@waitFinish true
        })
    }

    suspend fun getRootPaths(remoteId: String, replyCallback: (List<PathInfo>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        socket.send(
            header = SocketHeader(command = "rootPaths"),
            params = mapOf("replyKey" to replyKey.toString()),
            value = ""
        )

        val paths: MutableList<PathInfo> = mutableListOf()
        socket.waitFinish(replyKey, callback = {
            paths.addAll(socket.replyMessage[replyKey] as List<PathInfo>)
            true
        })

        replyCallback(paths)
        socket.replyMessage.remove(replyKey)
    }

    @OptIn(ExperimentalSerializationApi::class, ExperimentalStdlibApi::class)
    suspend fun getTraversePath(path: String, remoteId: String, replyCallback: (Result<List<FileSimpleInfo>>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        socket.send(
            header = SocketHeader(command = "traversePath"),
            params = mapOf("replyKey" to replyKey.toString(), "path" to path),
            value = ""
        )


        socket.waitFinish(replyKey, callback = {
            val decodeFromHexString =
                ProtoBuf.decodeFromByteArray<WebSocketResult<WebSocketResultMapListFileSimpleInfo>>(
                    socket.replyMessage[replyKey] as ByteArray
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

            socket.replyMessage.remove(replyKey)
            return@waitFinish true
        })
    }
}