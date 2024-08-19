package app.filemanager.service.handle

import app.filemanager.data.main.DrawerBookmark
import app.filemanager.service.WebSocketConnectService
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.random.Random

class BookmarkHandle(private val webSocketConnectService: WebSocketConnectService) {
    suspend fun getBookmark(remoteId: String, replyCallback: (List<DrawerBookmark>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        webSocketConnectService.send(
            command = "/bookmark",
            header = listOf(replyKey.toString(), remoteId),
            value = ""
        )

        for (i in 0..100) {
            delay(100)
            if (webSocketConnectService.replyMessage.contains(replyKey)) {
                break
            }
        }

        val bookmarks: MutableList<DrawerBookmark> = mutableListOf()
        if (webSocketConnectService.replyMessage[replyKey] != null) {
            bookmarks.addAll(webSocketConnectService.replyMessage[replyKey] as List<DrawerBookmark>)
        }

        replyCallback(bookmarks)
        webSocketConnectService.replyMessage.remove(replyKey)
    }
}