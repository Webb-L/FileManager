package app.filemanager.service.socket

interface SocketServer {
    suspend fun start(port: Int, callback: (clientId: String, message: SocketMessage) -> Unit)
    suspend fun stop()
    suspend fun send(clientId: String, data: ByteArray)
}