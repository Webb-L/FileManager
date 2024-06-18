package app.filemanager.service.handle

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.service.WebSocketConnectService
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
    suspend fun getList(path: String, remoteId: String, replyCallback: (List<FileSimpleInfo>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        webSocketConnectService.send(command = "/list", header = "$remoteId $replyKey", params = path, value = "")

        var tempMap = mapOf<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>()
        val fileSimpleInfos: MutableList<FileSimpleInfo> = mutableListOf()

        webSocketConnectService.waitFinish(replyKey, callback = {
            val tempList = webSocketConnectService.replyMessage[replyKey] as Triple<Int, Int, String>
            if (tempList.first != tempList.second) {
                return@waitFinish false
            }

            tempMap =
                ProtoBuf.decodeFromHexString<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>(tempList.third)

            return@waitFinish true
        })

        tempMap.forEach {
            it.value.forEach { fileSimpleInfo ->
                fileSimpleInfos.add(fileSimpleInfo.apply {
                    protocol = it.key.first
                    protocolId = it.key.second
                    this.path = path + this.path
                })
            }
        }

        replyCallback(fileSimpleInfos)
        webSocketConnectService.replyMessage.remove(replyKey)
    }

    suspend fun getRootPaths(remoteId: String, replyCallback: (List<String>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        webSocketConnectService.send(command = "/root_paths", header = "$remoteId $replyKey", value = "")

        val paths: MutableList<String> = mutableListOf()
        webSocketConnectService.waitFinish(replyKey, callback = {
            paths.addAll(webSocketConnectService.replyMessage[replyKey] as List<String>)
            true
        })

        replyCallback(paths)
        webSocketConnectService.replyMessage.remove(replyKey)
    }
}