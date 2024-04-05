package app.filemanager.service

import app.filemanager.data.file.FileInfo
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceType
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

expect class WebSocketService() {
    fun getNetworkIp(): List<String>
    suspend fun scanService(): List<String>
    fun startService()
    fun stopService()
}

const val SEND_SPLITE = "\n\n"

class WebSocketConnectService() : KoinComponent {
    private val manager by inject<WebSocketServiceManager>()
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
                send("/devices${SEND_SPLITE}")
                isConnected = true
            }
            launch {
                try {
                    for (message in incoming) {
                        message as? Frame.Text ?: continue
                        parseMessage(message.readText())
                    }
                } catch (e: Exception) {
                    manager.services.remove(this@WebSocketConnectService)
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
    suspend fun sendReplyList(id: String, content: List<FileInfo>) {
        send("/reply_list $id${SEND_SPLITE}${ProtoBuf.encodeToHexString(content)}".toByteArray())
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
    suspend fun getList(path: String, remoteId: String) {
        send("/list $path $remoteId\n\n".toByteArray())
    }


    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun parseMessage(msg: String) {
        val messages = msg.split("\n")
        val header = messages[0].split(" ")
        val content = messages[1]
        when (header[0]) {
            "/reply_devices" -> {
                for (message in content.split("\n")) {
                    val line = message.split(" ")
                    deviceState.devices.add(
                        Device(
                            id = line[0],
                            name = line[1],
                            host = line[2],
                            type = DeviceType.IOS
                        )
                    )

                    getList("/", line[0])
                }
            }

            // 远程设备需要我本地文件
            // TODO 检查权限
            "/list" -> sendReplyList(header[2], header[1].getFileAndFolder().map { fileInfo ->
                fileInfo.protocol = FileProtocol.Device
                fileInfo.protocolId = deviceId
                fileInfo
            })

            // 收到对方返回的文件文件夹信息
            "/reply_list" -> {
                println(ProtoBuf.decodeFromHexString<List<FileInfo>>(content))
            }

            else -> {
                println(header[0])
            }
        }
    }

    fun close() = client.close()
}