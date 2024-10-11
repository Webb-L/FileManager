package app.filemanager.service

import app.filemanager.service.response.DeviceResponse
import app.filemanager.ui.state.main.DeviceState
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.utils.io.core.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

expect class WebSocketService() {
    fun getNetworkIp(): List<String>
    suspend fun scanService(): List<String>
    fun startService()
    fun stopService()
}

const val SEND_SPLIT = "\n\n"

// 接受数据超时时间
const val TIMEOUT = 10_000
const val MAX_LENGTH = 6144

class WebSocketConnectService() : KoinComponent {
    internal val replyMessage = mutableMapOf<Long, Any>()

    internal val deviceState by inject<DeviceState>()
    private var deviceId: String = ""
    private var deviceName: String = ""


    private val client = HttpClient {
        install(WebSockets)
//        engine {
//            proxy = ProxyBuilder.http("http://127.0.0.1:8080")
//        }
    }
    private var session: DefaultClientWebSocketSession? = null

//    internal val pathHandle by lazy { PathHandle(this) }
//    internal val fileHandle by lazy { FileHandle(this) }
//    internal val bookmarkHandle by lazy { BookmarkHandle(this) }

//    private val pathRequest by lazy { PathRequest(this) }
//    private val fileRequest by lazy { FileRequest(this) }
//    private val bookmarkRequest by lazy { BookmarkRequest(this) }

    private val deviceResponse by lazy { DeviceResponse(this) }
//    private val pathResponse by lazy { PathResponse(this) }
//    private val fileResponse by lazy { FileResponse(this) }
//    private val bookmarkResponse by lazy { BookmarkResponse(this) }

    @OptIn(ExperimentalEncodingApi::class)
//    suspend fun connect(host: String) {
//        val settings by inject<Settings>()
//        deviceId = settings.getString("deviceId", "")
//        deviceName = settings.getString("deviceName", "")
//
//        client.webSocket(
//            method = HttpMethod.Get,
//            host = host,
//            port = 12040,
//            path = "/?id=$deviceId&name=${Base64.encode(deviceName.toByteArray())}",
//        ) {
//            session = this
//            launch {
//                delay(10)
//                send("/devices", header = listOf("", ""), value = "")
//            }
//            launch {
//                try {
//                    for (message in incoming) {
//                        message as? Frame.Text ?: continue
//                        parseMessage(message.readText())
//                    }
//                } catch (e: Exception) {
//                    println(e)
//                    close()
//                }
//            }.join()
//        }
//    }

    suspend fun sendFile(id: String, fileName: String) {
//        send("/upload 12132 $id$SEND_SPLIT".toByteArray().plus(FileUtils.getData(fileName, 0, 300)))
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun parseMessage(msg: String) {
        val messages = msg.split(SEND_SPLIT)
        val header = messages[0].split(" ")
        val headerCommand = header[0]
        val headerKey = if (header.size > 1) header[1].toLong() else -1
        val headerDevices = mutableListOf<String>()
        if (header.size > 2) {
            headerDevices.addAll(header[2].split(","))
        }
        println("header = $header")

        val params = messages[1].split(" ").map { String(Base64.decode(it)) }

        val content = messages[2]

        println("<<<<<< -------------------\nheader = [${messages[0]}]\nparams = [${messages[1]}]\ncontent = [$content]\n")

        when (headerCommand) {
            "/replyDevices" -> deviceResponse.deviceList(content)

            // 远程设备需要我本地文件
//            "/list" -> pathRequest.sendList(headerKey, header[2], params[0])

            // 收到对方返回的文件文件夹信息
//            "/replyList" -> pathResponse.replyList(headerKey, params, content)

            // 远程设备需要我本地的书签
//            "/bookmark" -> bookmarkRequest.sendBookmark(headerKey, header[2])

            // 收到对方返回的文件文件夹信息
//            "/replyBookmark" -> bookmarkResponse.replyBookmark(headerKey, content)

            // 远程设备需要我本地的书签
//            "/rootPaths" -> pathRequest.sendRootPaths(headerKey, header[2])

            // 收到对方返回的文件文件夹信息
//            "/replyRootPaths" -> pathResponse.replyRootPaths(headerKey, content)

            // 重命名文件和文件夹
//            "/rename" -> fileRequest.sendRename(content)

            // 创建文件夹
//            "/createFolder" -> fileRequest.sendCreateFolder(headerKey, header[2], params)

            // 收到对方返回创建文件夹结果
//            "/replyCreateFolder" -> fileResponse.replyCreateFolder(headerKey, content)

            // 遍历目录下所有文件夹和文件
//            "/traversePath" -> pathRequest.sendTraversePath(headerKey, header[2], params)
            // 收到对方发送过来的遍历目录下文件夹和文件
//            "/replyTraversePath" -> pathResponse.replyTraversePath(headerKey, params, content)
            else -> {
                println("匹配不到指令：\n${header[0]}")
            }
        }
    }

    /**
     * 发送消息
     * 使用：send(指令, 设备1,设备2,... 消息标识, 参数, 发送的消息)
     *
     * @param command 消息指令
     * @param header 消息头 设备, 消息标识
     * @param params 消息参数 /home/user1/1.txt /home/user2/2.txt
     * @param value 内容
     */
    @OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
    internal suspend inline fun <reified T> send(
        command: String,
        header: List<String> = emptyList(),
        params: List<String> = emptyList(),
        value: T
    ) {
        val content: String = if (value is String) {
            value
        } else {
            if (value == null) "" else ProtoBuf.encodeToHexString(value)
        }

        val headerString = header.joinToString(" ") { it }

        if (content.length < MAX_LENGTH) {
            val paramsString = params.joinToString(" ") { Base64.encode(it.toByteArray()) }
            session?.send("$command $headerString${SEND_SPLIT}$paramsString${SEND_SPLIT}${content}".toByteArray())

            println(
                "------------------- >>>>>>\nheader = $command [$headerString]\nparams = [$paramsString]\ncontent = [$content]\n",
            )
            return
        }

        var index = 1
        val chunked = content.chunked(MAX_LENGTH)
        chunked.forEach {
            val paramsString = (listOf(
                index.toString(),
                chunked.size.toString()
            ) + params).joinToString(" ") { Base64.encode(it.toByteArray()) }

            session?.send("$command $headerString${SEND_SPLIT}$paramsString${SEND_SPLIT}${it}".toByteArray())

            println(
                "------------------- >>>>>>\nheader = $command [$headerString]\nparams = [$paramsString]\ncontent = [$it]\n",
            )
            index++
        }
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

    fun close() = client.close()
}