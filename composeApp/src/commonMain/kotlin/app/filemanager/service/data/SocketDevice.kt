package app.filemanager.service.data

import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceType
import app.filemanager.service.SocketClientManger
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable


@Serializable
enum class ConnectType(type: String) {
    New("New"),
    Connect("Connect"),
    Fail("Fail"),
    UnConnect("UnConnect"),
    Loading("Loading"),
    Rejected("Rejected"),
}

@Serializable
data class SocketDevice(
    val id: String,
    val name: String,
    var host: String = "",
    val type: DeviceType,
    var connectType: ConnectType = ConnectType.New,
) {
    @Contextual
    var socketManger: SocketClientManger? = null

    fun toDevice(): Device {
        return Device(id = id, name = name, host = mutableMapOf(host to socketManger!!), type = type)
    }

    fun withCopy(
        id: String = this.id,
        name: String = this.name,
        host: String = this.host,
        type: DeviceType = this.type,
        connectType: ConnectType = this.connectType,
        socketManger: SocketClientManger? = this.socketManger
    ): SocketDevice {
        return SocketDevice(id, name, host, type, connectType).apply {
            this.socketManger = socketManger
        }
    }
}