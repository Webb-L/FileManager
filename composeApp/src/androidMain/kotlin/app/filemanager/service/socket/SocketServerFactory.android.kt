package app.filemanager.service.socket

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ServerSocket
import java.net.Socket
import kotlin.coroutines.CoroutineContext

class JvmSocketServer(private val coroutineContext: CoroutineContext = Dispatchers.IO) : SocketServer {
    private var serverSocket: ServerSocket? = null
    private val clientHandlers = mutableMapOf<String, ClientHandler>()
    private val clientMessageListeners = mutableMapOf<String, (ByteArray) -> Unit>()

    override suspend fun start(port: Int) {
        withContext(coroutineContext) {
            serverSocket = ServerSocket(port)
            while (true) {
                val clientSocket = serverSocket?.accept()
                val clientId = clientSocket?.remoteSocketAddress?.toString() ?: "unknown"
                val clientHandler = ClientHandler(clientSocket!!, clientId)
                clientHandlers[clientId] = clientHandler
                launch { clientHandler.handle() }
            }
        }
    }

    override suspend fun stop() {
        withContext(coroutineContext) {
            serverSocket?.close()
            serverSocket = null
            clientHandlers.values.forEach { it.close() }
        }
    }

    override suspend fun send(clientId: String, data: ByteArray) {
        withContext(coroutineContext) {
            clientHandlers[clientId]?.send(data)
        }
    }

    override fun onClientMessageReceived(clientId: String, message: (ByteArray) -> Unit) {
        clientMessageListeners[clientId] = message
    }

    inner class ClientHandler(private val socket: Socket, private val clientId: String) {
        private val inputStream = socket.getInputStream()
        private val outputStream = socket.getOutputStream()

        suspend fun handle() {
            val buffer = ByteArray(1024)
            while (true) {
                val bytesRead = withContext(Dispatchers.IO) {
                    inputStream.read(buffer)
                }
                if (bytesRead == -1) break
                val message = buffer.copyOf(bytesRead)
                clientMessageListeners[clientId]?.invoke(message)
            }
            close()
        }

        fun send(data: ByteArray) {
            outputStream.write(data)
        }

        fun close() {
            socket.close()
        }
    }
}

actual fun createSocketServer(): SocketServer {
    return JvmSocketServer()
}