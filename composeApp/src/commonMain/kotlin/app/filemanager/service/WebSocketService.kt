package app.filemanager.service

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.utils.FileUtils
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.random.Random

expect class WebSocketService() {
    fun getNetworkIp(): List<String>
    suspend fun scanService(): List<String>
    fun startService()
    fun stopService()
}

const val SEND_SPLIT = "\n\n"

class WebSocketConnectService() : KoinComponent {
    private val replyMessage = mutableMapOf<Long, Any>()

    private val deviceState by inject<DeviceState>()
    private var deviceId: String = ""
    private var deviceName: String = ""


    private val client = HttpClient {
        install(WebSockets)
    }
    private var session: DefaultClientWebSocketSession? = null

    var isConnected = false

    suspend fun connect(host: String) {
        val settings by inject<Settings>()
        deviceId = settings.getString("deviceId", "")
        deviceName = settings.getString("deviceName", "")

        client.webSocket(
            method = HttpMethod.Get,
            host = host,
            port = 12040,
            path = "/?id=$deviceId&name=$deviceName"
        ) {
            session = this
            launch {
                delay(10)
                send("/devices${SEND_SPLIT}")
                isConnected = true
            }
            launch {
                try {
                    for (message in incoming) {
                        message as? Frame.Text ?: continue
                        parseMessage(message.readText())
                    }
                } catch (e: Exception) {
                    isConnected = false
                    println(e)
                    close()
                }
            }.join()
        }
    }

    private suspend fun send(content: ByteArray) {
        if (isConnected) {
            session?.send(String(content))
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sendReplyList(id: String, replyKey: Long, directory: String) {
        val sendFileSimpleInfos = mutableMapOf<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>().apply {
            directory.getFileAndFolder().forEach { fileSimpleInfo ->
                val key = if (fileSimpleInfo.protocol == FileProtocol.Local)
                    Pair(FileProtocol.Device, fileSimpleInfo.protocolId)
                else
                    Pair(fileSimpleInfo.protocol, fileSimpleInfo.protocolId)

                if (!containsKey(key)) {
                    put(key, mutableListOf(fileSimpleInfo.apply {
                        this.path = this.path.replace(directory, "")
                        this.protocol = FileProtocol.Local
                        this.protocolId = ""
                    }))
                } else {
                    get(key)?.add(fileSimpleInfo.apply {
                        this.path = this.path.replace(directory, "")
                        this.protocol = FileProtocol.Local
                        this.protocolId = ""
                    })
                }
            }
        }

        send("/reply_list $id $replyKey${SEND_SPLIT}${ProtoBuf.encodeToHexString(sendFileSimpleInfos)}".toByteArray())
    }

    suspend fun sendFile(id: String, fileName: String) {
        send("/upload 12132 $id\n\n".toByteArray().plus(FileUtils.getData(fileName, 0, 300)))
    }

    /**
     * 从远程设备获取指定路径下的文件和文件夹列表。
     *
     * @param path 要获取列表的路径。
     * @param remoteId 远程设备的ID。
     */
    suspend fun getList(path: String, remoteId: String, replyListCallback: (List<FileSimpleInfo>) -> Unit) {
        val currentInstant: Instant = Clock.System.now()
        val replyKey = currentInstant.toEpochMilliseconds() + Random.nextInt()
        send("/list $path $remoteId $replyKey\n\n".toByteArray())

        for (i in 0..100) {
            delay(100)
            if (replyMessage.contains(replyKey)) {
                break
            }
        }

        val fileSimpleInfos: MutableList<FileSimpleInfo> = mutableListOf()
        if (replyMessage[replyKey] != null) {
            val tempMap = replyMessage[replyKey] as Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>
            tempMap.forEach {
                it.value.forEach { fileSimpleInfo ->
                    fileSimpleInfos.add(fileSimpleInfo.apply {
                        protocol = it.key.first
                        protocolId = it.key.second
                        this.path = path + this.path
                    })
                }
            }
        }

        replyListCallback(fileSimpleInfos)
        replyMessage.remove(replyKey)
    }


    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun parseMessage(msg: String) {
        val messages = msg.split("\n")
        val header = messages[0].split(" ")
        val content = messages[1]
        when (header[0]) {
            "/reply_devices" -> {
                for (message in content.split("\n")) {
                    deviceState.addDevices(
                        message,
                        this,
                    )
                }
            }

            // 远程设备需要我本地文件
            // TODO 检查权限
            "/list" -> sendReplyList(
                header[2],
                header[3].toLong(),
                header[1]
            )

            // 收到对方返回的文件文件夹信息
            "/reply_list" -> replyMessage[header[1].toLong()] =
                ProtoBuf.decodeFromHexString<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>(content)

            else -> {
                println(header[0])
            }
        }
    }

    fun close() = client.close()
}