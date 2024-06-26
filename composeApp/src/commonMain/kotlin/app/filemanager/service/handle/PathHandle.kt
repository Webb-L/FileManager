package app.filemanager.service.handle

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.service.WebSocketConnectService
import app.filemanager.service.WebSocketResult
import app.filemanager.service.WebSocketResultMapListFileSimpleInfo
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.random.Random


class PathHandle(private val webSocketConnectService: WebSocketConnectService) {
    /**
     * 从远程设备获取指定路径下的文件和文件夹列表。
     *
     * @param path 要获取列表的路径。
     * @param remoteId 远程设备的ID。
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getList(path: String, remoteId: String, replyCallback: (Result<List<FileSimpleInfo>>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        webSocketConnectService.send(command = "/list", header = "$remoteId $replyKey", params = path, value = "")

        val fileSimpleInfos: MutableList<FileSimpleInfo> = mutableListOf()

        webSocketConnectService.waitFinish(replyKey, callback = {
            val tempList = webSocketConnectService.replyMessage[replyKey] as Triple<Int, Int, String>
            if (tempList.first != tempList.second) {
                return@waitFinish false
            }

            val decodeFromHexString =
                ProtoBuf.decodeFromHexString<WebSocketResult<WebSocketResultMapListFileSimpleInfo>>(
                    tempList.third
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

            webSocketConnectService.replyMessage.remove(replyKey)
            return@waitFinish true
        })
    }

    suspend fun getRootPaths(remoteId: String, replyCallback: (List<String>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        webSocketConnectService.send(command = "/rootPaths", header = "$remoteId $replyKey", value = "")

        val paths: MutableList<String> = mutableListOf()
        webSocketConnectService.waitFinish(replyKey, callback = {
            paths.addAll(webSocketConnectService.replyMessage[replyKey] as List<String>)
            true
        })

        replyCallback(paths)
        webSocketConnectService.replyMessage.remove(replyKey)
    }
}