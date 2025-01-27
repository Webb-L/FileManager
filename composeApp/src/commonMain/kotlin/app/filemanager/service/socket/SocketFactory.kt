package app.filemanager.service.socket

import app.filemanager.service.BaseSocketManager.Companion.SEND_IDENTIFIER
import app.filemanager.service.data.SocketDevice
import org.koin.core.component.KoinComponent

interface Socket : KoinComponent {
    suspend fun send(clientId: String, data: ByteArray)
    fun findIdentifierIndex(buffer: ByteArray): Int {
        for (i in 0..buffer.size - SEND_IDENTIFIER.size) {
            var match = true
            for (j in SEND_IDENTIFIER.indices) {
                if (buffer[i + j] != SEND_IDENTIFIER[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                return i
            }
        }
        return -1
    }
}

interface SocketClient : Socket {
    suspend fun connect(host: String, port: Int, receive: (data: SocketMessage) -> Unit)

    override suspend fun send(clientId: String, data: ByteArray)
    fun disconnect(): Boolean

    suspend fun getAllIPAddresses(): List<String>

    suspend fun scanner(address: List<String>, callback: (SocketDevice) -> Unit)
}

interface SocketServer : Socket {
    suspend fun start(port: Int, callback: (clientId: String, message: SocketMessage) -> Unit)
    suspend fun stop()

    override suspend fun send(clientId: String, data: ByteArray)
}

expect fun createSocketServer(): SocketServer
expect fun createSocketClient(): SocketClient