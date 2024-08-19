package app.filemanager.service.socket

import app.filemanager.createSettings
import app.filemanager.data.main.DeviceType
import app.filemanager.service.MAX_LENGTH
import app.filemanager.service.data.SocketDevice
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class JvmSocketServer(private val coroutineContext: CoroutineContext = Dispatchers.IO) : SocketServer {
    private val settings = createSettings()
    private val devices = mutableMapOf<String, Pair<SocketDevice, ByteWriteChannel>>()

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun start(port: Int, callback: (clientId: String, message: SocketMessage) -> Unit) {
        withContext(coroutineContext) {
            val selectorManager = SelectorManager(Dispatchers.IO)
            val serverSocket = aSocket(selectorManager).tcp().bind("127.0.0.1", 1204)
            println("Server is listening at ${serverSocket.localAddress}")
            while (true) {
                val socket = serverSocket.accept()
                println("Accepted ${socket.remoteAddress}")
                launch {
                    val receiveChannel = socket.openReadChannel()
                    val sendChannel = socket.openWriteChannel(autoFlush = true)

                    try {
                        val buffer = ByteArray(MAX_LENGTH)
                        while (true) {
                            val bufferLength = receiveChannel.readAvailable(buffer)
                            if (bufferLength == -1) {
                                break
                            }

                            val socketMessage = ProtoBuf.decodeFromByteArray<SocketMessage>(buffer.copyOf(bufferLength))
                            if (socketMessage.header.command == "connect") {
                                devices[socket.remoteAddress.toString()] = Pair(
                                    ProtoBuf.decodeFromByteArray<SocketDevice>(
                                        socketMessage.body
                                    ),
                                    sendChannel
                                )
                                sendChannel.writeFully(
                                    ProtoBuf.encodeToByteArray(
                                        SocketMessage.sendDevice(
                                            deviceId = settings.getString("deviceId", ""),
                                            deviceName = settings.getString("deviceName", ""),
                                            host = socket.localAddress.toString(),
                                            type = DeviceType.JVM
                                        )
                                    )
                                )
                            } else {
                                callback(socket.remoteAddress.toString(), socketMessage)
                            }
                        }
                    } catch (e: IOException) {
                        println("Error handling connection: ${e.message}")
                    } finally {
                        println("Closing socket")
                        withContext(Dispatchers.IO) {
                            socket.close()
                        }
                    }
                }
            }
        }
    }


    override suspend fun stop() {
    }

    override suspend fun send(clientId: String, data: ByteArray) {
        val deviceSendChannel = devices[clientId] ?: return
        println("server - send = ${data.size}")
        deviceSendChannel.second.writeFully(data)
    }
}


actual fun createSocketServer(): SocketServer {
    return JvmSocketServer()
}


class JvmSocketClient(private val coroutineContext: CoroutineContext = Dispatchers.IO) : SocketClient {
    private val settings = createSettings()

    private var sendChannel: ByteWriteChannel? = null

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun connect(host: String, port: Int, receive: (data: SocketMessage) -> Unit) {
        withContext(coroutineContext) {
            val selectorManager = SelectorManager(Dispatchers.IO)
            val socket = aSocket(selectorManager).tcp().connect(host, port)

            val receiveChannel = socket.openReadChannel()
            sendChannel = socket.openWriteChannel(autoFlush = true)
            send(
                SocketMessage.sendDevice(
                    deviceId = settings.getString("deviceId", ""),
                    deviceName = settings.getString("deviceName", ""),
                    host = socket.localAddress.toString(),
                    type = DeviceType.JVM
                )
            )


            val buffer = ByteArray(MAX_LENGTH+1024)
            while (true) {
                val bufferLength = receiveChannel.readAvailable(buffer)
                if (bufferLength == -1) {
                    break
                }
                println("client = $bufferLength")
                val socketMessage = ProtoBuf.decodeFromByteArray<SocketMessage>(buffer.copyOf(bufferLength))
                receive(socketMessage)
            }
        }
    }

    // 发送消息的方法
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun send(data: SocketMessage) {
        sendChannel?.writeFully(ProtoBuf.encodeToByteArray(data))
    }

    override suspend fun disconnect() {
    }

    override suspend fun scanner(address: List<String>): MutableList<SocketDevice> {
        val socketDevices = mutableListOf<SocketDevice>()
        val executor = Executors.newFixedThreadPool(100)

        for (ip in address) {
            executor.submit {
                scanPort(ip)?.let {
                    it.host = ip
                    socketDevices.add(it)
                }
            }
        }
        executor.shutdown()

        try {
            withContext(Dispatchers.IO) {
                executor.awaitTermination(1, TimeUnit.HOURS)
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return socketDevices
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun scanPort(ip: String): SocketDevice? {
//        try {
//            Socket().use { socket ->
//                socket.connect(InetSocketAddress(ip, 1204), 200)
//
//                val deviceId = settings.getString("deviceId", "")
//                val deviceName = settings.getString("deviceName", "")
//
//                // 通过输出流发送数据
//                socket.getOutputStream().write(
//                    ProtoBuf.encodeToByteArray(
//                        SocketMessage(
//                            header = SocketHeader(command = "connect"),
//                            params = mapOf(),
//                            body = ProtoBuf.encodeToByteArray(
//                                SocketDevice(
//                                    id = deviceId,
//                                    name = deviceName,
//                                    host = socket.localSocketAddress.address,
//                                    type = DeviceType.JVM
//                                )
//                            )
//                        )
//                    )
//                )
//
//                socket.getOutputStream().flush()
//
//                val buffer = ByteArray(1024)
//                val bufferLength = socket.getInputStream().read(buffer)
//                if (bufferLength != -1) {
//                    socket.close()
//                    val message = buffer.copyOf(bufferLength)
//                    val socketMessage =
//                        ProtoBuf.decodeFromByteArray<SocketMessage>(message)
//                    return ProtoBuf.decodeFromByteArray(socketMessage.body) as SocketDevice
//                }
//                socket.close()
//            }
//        } catch (e: Exception) {
//            println("Error scanPort with $ip: ${e.message}")
//        }
        return null
    }
}

actual fun createSocketClient(): SocketClient {
    return JvmSocketClient()
}