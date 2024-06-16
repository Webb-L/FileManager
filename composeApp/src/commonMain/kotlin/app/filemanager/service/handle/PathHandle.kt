package app.filemanager.service.handle

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.service.TIMEOUT
import app.filemanager.service.WebSocketConnectService
import kotlinx.coroutines.delay
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

        var startTime = Clock.System.now().toEpochMilliseconds()
        var endTime = startTime + TIMEOUT

        while (true) {
            if (Clock.System.now().toEpochMilliseconds() < endTime) {
                if (webSocketConnectService.replyMessage.contains(replyKey)) {
                    startTime = Clock.System.now().toEpochMilliseconds()
                    endTime = startTime + TIMEOUT
                    val temp =
                        webSocketConnectService.replyMessage[replyKey] as Triple<Int, Int, String>


                    // 结束
                    if (temp.first == temp.second) {
                        println("length = ${temp.third.length}")
                        tempMap =
                            ProtoBuf.decodeFromHexString<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>(
                                temp.third
                            )
                        break
                    }
                }
            } else {
                break
            }
            delay(100)
        }

        tempMap.forEach {
            it.value.forEach { fileSimpleInfo ->
                fileSimpleInfos.add(fileSimpleInfo.apply {
                    protocol = it.key.first
                    protocolId = it.key.second
                    this.path = path + this.path
                })
            }
        }

        println("length = ${fileSimpleInfos.size}")

        replyCallback(fileSimpleInfos)
        webSocketConnectService.replyMessage.remove(replyKey)
    }

    suspend fun getRootPaths(remoteId: String, replyCallback: (List<String>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        webSocketConnectService.send(command = "/root_paths", header = "$remoteId $replyKey", value = "")

        for (i in 0..100) {
            delay(100)
            if (webSocketConnectService.replyMessage.contains(replyKey)) {
                break
            }
        }

        val paths: MutableList<String> = mutableListOf()
        if (webSocketConnectService.replyMessage[replyKey] != null) {
            paths.addAll(webSocketConnectService.replyMessage[replyKey] as List<String>)
        }

        replyCallback(paths)
        webSocketConnectService.replyMessage.remove(replyKey)
    }
}