package app.filemanager.service.handle

import app.filemanager.service.WebSocketConnectService
import app.filemanager.service.WebSocketResult
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.random.Random

class FileHandle(private val webSocketConnectService: WebSocketConnectService) {
    /**
     * 重命名文件和文件夹
     *
     * @param remoteId
     * @param path
     * @param oldName
     * @param newName
     */
    suspend fun rename(remoteId: String, path: String, oldName: String, newName: String) {
        webSocketConnectService.send(
            command = "/rename",
            header = listOf(remoteId),
            params = listOf(path, oldName, newName),
            value = ""
        )
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
        webSocketConnectService.send(
            command = "/createFolder",
            header = listOf(replyKey.toString(), remoteId),
            params = listOf(path, name),
            value = ""
        )
        webSocketConnectService.waitFinish(replyKey, callback = {
            val decodeFromHexString =
                ProtoBuf.decodeFromHexString<WebSocketResult<Boolean>>(
                    webSocketConnectService.replyMessage[replyKey] as String
                )

            if (decodeFromHexString.isSuccess) {
                replyCallback(Result.success(decodeFromHexString.value ?: false))
            } else {
                replyCallback(Result.failure(decodeFromHexString.deSerializable()))
            }

            webSocketConnectService.replyMessage.remove(replyKey)
            return@waitFinish true
        })
    }
}