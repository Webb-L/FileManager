package app.filemanager.service.socket

import app.filemanager.service.data.SocketDevice

interface Socket {
    suspend fun send(clientId: String, data: ByteArray)
}

interface SocketClient : Socket {
    suspend fun connect(host: String, port: Int, receive: (data: SocketMessage) -> Unit)

    override suspend fun send(clientId: String, data: ByteArray)
    suspend fun disconnect()

    suspend fun scanner(address: List<String>): List<SocketDevice>
}

interface SocketServer : Socket {
    suspend fun start(port: Int, callback: (clientId: String, message: SocketMessage) -> Unit)
    suspend fun stop()
    override suspend fun send(clientId: String, data: ByteArray)
}

expect fun createSocketServer(): SocketServer
expect fun createSocketClient(): SocketClient