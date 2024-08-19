package app.filemanager.service.socket

import app.filemanager.data.main.DeviceType
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.SocketDevice
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

@Serializable
data class SocketMessage(
    val header: SocketHeader,
    val params: Map<String, String> = mapOf(),
    val body: ByteArray = byteArrayOf(),
) {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun sendDevice(
            deviceId: String,
            deviceName: String,
            host: String,
            type: DeviceType = DeviceType.JVM,
        ): SocketMessage {
            return SocketMessage(
                header = SocketHeader(command = "connect", devices = listOf()),
                params = mapOf(),
                body = ProtoBuf.encodeToByteArray(
                    SocketDevice(
                        id = deviceId,
                        name = deviceName,
                        host = host,
                        type = type,
                        connectType = ConnectType.UnConnect
                    )
                )
            )
        }
    }
}

@Serializable
data class SocketHeader(
    val command: String,
    val devices: List<String> = listOf()
)
