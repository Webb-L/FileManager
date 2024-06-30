package app.filemanager.service.response

import app.filemanager.data.file.PathInfo
import app.filemanager.service.WebSocketConnectService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.protobuf.ProtoBuf

class PathResponse(private val webSocketConnectService: WebSocketConnectService) {
    // 收到对方返回的文件文件夹信息
    fun replyList(headerKey: Long, params: List<String>, content: String) {
        MainScope().launch {
            if (params.isEmpty() || params.size < 2) {
                webSocketConnectService.replyMessage[headerKey] = Triple(0, 0, content)
                return@launch
            }
            if (!webSocketConnectService.replyMessage.containsKey(headerKey)) {
                webSocketConnectService.replyMessage[headerKey] = Triple(
                    params[0].toInt(),
                    params[1].toInt(),
                    content
                )
            } else {
                val temp =
                    webSocketConnectService.replyMessage[headerKey] as Triple<Int, Int, String>

                webSocketConnectService.replyMessage[headerKey] = Triple(
                    params[0].toInt(),
                    params[1].toInt(),
                    temp.third + content
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun replyRootPaths(headerKey: Long, content: String) {
        MainScope().launch {
            webSocketConnectService.replyMessage[headerKey] = ProtoBuf.decodeFromHexString<List<PathInfo>>(content)
        }
    }

    fun replyTraversePath(headerKey: Long, params: List<String>, content: String) {

    }
}