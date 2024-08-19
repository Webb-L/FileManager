package app.filemanager.service.data

import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceType
import app.filemanager.service.SocketClientManger
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable


@Serializable
enum class ConnectType(type: String) {
    Connect("Connect"),
    Fail("Fail"),
    UnConnect("UnConnect"),
    Loading("Loading"),
}

@Serializable
data class SocketDevice(
    val id: String,
    val name: String,
    var host: String = "",
    val type: DeviceType,
    var connectType: ConnectType = ConnectType.UnConnect,
) {
    @Contextual
    var socketManger: SocketClientManger? = null
    fun toDevice(): Device {
        return Device(id = id, name = name, host = mutableMapOf(host to socketManger!!), type = type)
    }
}