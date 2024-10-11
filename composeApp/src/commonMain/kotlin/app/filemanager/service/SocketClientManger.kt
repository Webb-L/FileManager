package app.filemanager.service

import app.filemanager.service.data.SocketDevice
import app.filemanager.service.handle.BookmarkHandle
import app.filemanager.service.handle.FileHandle
import app.filemanager.service.handle.PathHandle
import app.filemanager.service.response.BookmarkResponse
import app.filemanager.service.response.FileResponse
import app.filemanager.service.response.PathResponse
import app.filemanager.service.socket.SocketClient
import app.filemanager.service.socket.SocketHeader
import app.filemanager.service.socket.SocketMessage
import app.filemanager.service.socket.createSocketClient
import app.filemanager.ui.state.main.DeviceState
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.io.encoding.ExperimentalEncodingApi

class SocketClientManger : KoinComponent {
    private val socket: SocketClient = createSocketClient()
    internal val replyMessage = mutableMapOf<Long, Any>()
    internal val deviceState by inject<DeviceState>()

    internal val pathHandle by lazy { PathHandle(this) }
    internal val fileHandle by lazy { FileHandle(this) }
    internal val bookmarkHandle by lazy { BookmarkHandle(this) }

    private val pathResponse by lazy { PathResponse(this) }
    private val fileResponse by lazy { FileResponse(this) }
    private val bookmarkResponse by lazy { BookmarkResponse(this) }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun connect(host: String = "127.0.0.1", port: Int = 1204) {
        socket.connect(host, port) { message ->
            when (message.header.command) {
                "connect" -> {
                    val socketDevice = ProtoBuf.decodeFromByteArray<SocketDevice>(message.body)
                    socketDevice.socketManger = this@SocketClientManger
                    deviceState.socketDevices.add(socketDevice)
                }

                "replyRootPaths" -> pathResponse.replyRootPaths(message)
                "replyList" -> pathResponse.replyList(message)
                "replyBookmark" -> bookmarkResponse.replyBookmark(message)
                "replyRename" -> fileResponse.replyRename(message)
                "replyCreateFolder" -> fileResponse.replyCreateFolder(message)
                "replyGetSizeInfo" -> fileResponse.replyGetSizeInfo(message)

                else -> {
                    println("未能匹配上：$message")
                }
            }
        }
    }

    /**
     * 发送消息
     * 使用：send(指令, 设备1,设备2,... 消息标识, 参数, 发送的消息)
     */
    @OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
    internal suspend inline fun <reified T> send(
        header: SocketHeader,
        params: Map<String, String> = mapOf(),
        value: T
    ) {
        val byteArray = ProtoBuf.encodeToByteArray(value)


        if (byteArray.size < MAX_LENGTH) {
            socket.send(
                SocketMessage(
                    header = header,
                    params = params,
                    body = byteArray
                )
            )
//
//            println(
//                "------------------- >>>>>>\nheader = $command [$headerString]\nparams = [$paramsString]\ncontent = [$content]\n",
//            )
            return
        }

//        var index = 1
//        val chunked = byteArray.chunked(MAX_LENGTH)
//        chunked.forEach {
//            val paramsString = (listOf(index.toString(), chunked.size.toString()) + params)
//                .map { Base64.encode(it.toByteArray()) }.joinToString(" ")
//
//            session?.send("$command $headerString${SEND_SPLIT}$paramsString${SEND_SPLIT}${it}".toByteArray())
//
//            println(
//                "------------------- >>>>>>\nheader = $command [$headerString]\nparams = [$paramsString]\ncontent = [$it]\n",
//            )
//            index++
//        }
    }

    /**
     * Wait finish
     *
     * @param replyKey
     * @param callback
     * @receiver
     */
    internal suspend fun waitFinish(replyKey: Long, callback: () -> Boolean) {
        var startTime = Clock.System.now().toEpochMilliseconds()
        var endTime = startTime + TIMEOUT

        while (true) {
            if (Clock.System.now().toEpochMilliseconds() < endTime) {
                if (replyMessage.contains(replyKey)) {
                    startTime = Clock.System.now().toEpochMilliseconds()
                    endTime = startTime + TIMEOUT

                    if (callback()) {
                        break
                    }
                }
            } else {
                break
            }
            delay(100)
        }
    }
}