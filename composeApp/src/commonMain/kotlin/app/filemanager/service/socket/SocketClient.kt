package app.filemanager.service.socket

import app.filemanager.service.data.SocketDevice

interface SocketClient {
    suspend fun connect(host: String, port: Int, receive: (data: SocketMessage) -> Unit)

    suspend fun send(data: SocketMessage)
    suspend fun disconnect()

    suspend fun scanner(address: List<String>): List<SocketDevice>
}