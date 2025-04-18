package app.filemanager.service.data

import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceType
import app.filemanager.data.main.Share
import app.filemanager.service.rpc.RpcClientManager
import app.filemanager.service.rpc.RpcClientManager.Companion.PORT
import app.filemanager.service.rpc.RpcShareClientManager
import app.filemanager.utils.serializer.SocketDeviceSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
enum class ConnectType(type: String) {
    New("New"),
    Connect("Connect"),
    Fail("Fail"),
    UnConnect("UnConnect"),
    Loading("Loading"),
    Rejected("Rejected"),
}

@Serializable(with = SocketDeviceSerializer::class)
data class SocketDevice(
    val id: String,
    var name: String,
    var host: String = "",
    var port: Int = PORT,
    val type: DeviceType,
    var connectType: ConnectType = ConnectType.New,
    @Transient
    var token: String = ""
) {
    @Transient
    var client: RpcClientManager? = null

    fun toDevice(includeHost: Boolean = true): Device {
        return Device(
            id = id,
            name = name,
            host = if (includeHost) mutableMapOf(host to client!!) else mutableMapOf(),
            type = type,
            token = token
        )
    }

    fun toShare(client: RpcShareClientManager): Share {
        return Share(
            id = id,
            name = name,
            type = type,
            rpcClientManager = client,
        )
    }

    fun withCopy(
        id: String = this.id,
        name: String = this.name,
        host: String = this.host,
        port: Int = this.port,
        type: DeviceType = this.type,
        connectType: ConnectType = this.connectType,
        token: String = this.token,
        client: RpcClientManager? = this.client
    ): SocketDevice {
        return SocketDevice(id, name, host, port, type, connectType, token).apply {
            this.client = client
        }
    }
}