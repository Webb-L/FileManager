package app.filemanager.service.response

import app.filemanager.data.main.DrawerBookmark
import app.filemanager.service.WebSocketConnectService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.protobuf.ProtoBuf

class BookmarkResponse(private val webSocketConnectService: WebSocketConnectService) {
    @OptIn(ExperimentalSerializationApi::class)
    fun replyBookmark(headerKey: Long, content: String) {
        MainScope().launch {
            webSocketConnectService.replyMessage[headerKey] =
                ProtoBuf.decodeFromHexString<List<DrawerBookmark>>(content)
        }
    }
}