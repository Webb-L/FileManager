package app.filemanager.service.handle

import app.filemanager.data.main.DrawerBookmark
import app.filemanager.service.SocketClientManger
import app.filemanager.service.WebSocketResult
import app.filemanager.service.socket.SocketHeader
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.random.Random

class BookmarkHandle(private val socket: SocketClientManger) {
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getBookmark(remoteId: String, replyCallback: (Result<List<DrawerBookmark>>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        socket.send(
            header = SocketHeader(command = "bookmark"),
            params = mapOf("replyKey" to replyKey.toString()),
            value = ""
        )


        socket.waitFinish(replyKey, callback = { result ->
            if (result.isFailure) {
                replyCallback(Result.failure(result.exceptionOrNull() ?: Exception()))
                return@waitFinish
            }

            val webSocketResult = ProtoBuf.decodeFromByteArray<WebSocketResult<List<DrawerBookmark>>>(
                result.getOrDefault(byteArrayOf())
            )
            if (webSocketResult.isSuccess) {
                val bookmarks: List<DrawerBookmark> = webSocketResult.value as List<DrawerBookmark>
                replyCallback(Result.success(bookmarks))
            } else {
                replyCallback(Result.failure(webSocketResult.deSerializable()))
            }
        })

//        for (i in 0..100) {
//            delay(100)
//            if (webSocketConnectService.replyMessage.contains(replyKey)) {
//                break
//            }
//        }
//
//        val bookmarks: MutableList<DrawerBookmark> = mutableListOf()
//        if (webSocketConnectService.replyMessage[replyKey] != null) {
//            bookmarks.addAll(webSocketConnectService.replyMessage[replyKey] as List<DrawerBookmark>)
//        }
//
//        replyCallback(bookmarks)
//        webSocketConnectService.replyMessage.remove(replyKey)
    }
}