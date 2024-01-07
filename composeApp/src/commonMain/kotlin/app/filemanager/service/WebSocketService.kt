package app.filemanager.service

import app.filemanager.data.file.DeviceType
import app.filemanager.data.main.Device
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

expect class WebSocketService() {
    fun getNetworkIp(): List<String>
    suspend fun scanService(): List<String>
    fun startService()
    fun stopService()
}

class WebSocketConnectService() : KoinComponent {
    private val manager by inject<WebSocketServiceManager>()
    private val deviceState by inject<DeviceState>()


    private val client = HttpClient {
        install(WebSockets)
    }
    private var session: DefaultClientWebSocketSession? = null

    var isConnected = false

    suspend fun connect(host: String) {
        val settings by inject<Settings>()
        val deviceId = settings.getString("deviceId", "")
        val deviceName = settings.getString("deviceName", "")

        client.webSocket(
            method = HttpMethod.Get,
            host = host,
            port = 12040,
            path = "/?id=$deviceId&name=$deviceName"
        ) {
            session = this
            launch {
                delay(10)
                send("/devices\n\n")
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

    suspend fun sendFile(id: String, fileName: String) {
        val header = "/upload 12132 $id\n\n"
        send(header.toByteArray().plus(FileUtils.getData(fileName, 0, 300)))
    }

    private fun parseMessage(msg: String) {
        val header = msg.indexOf("\n")
        val command = msg.substring(0, header)
        val content = msg.substring(header + 1, msg.length)
        when (command) {
            "===devices===" -> {
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
                }
            }

            else -> {}
        }
    }

    fun close() = client.close()
}