package app.filemanager.service.socket

import app.filemanager.createSettings
import app.filemanager.data.main.DeviceConnectType.*
import app.filemanager.data.main.DeviceType
import app.filemanager.db.FileManagerDatabase
import app.filemanager.extensions.getSubnetIps
import app.filemanager.service.BaseSocketManager.Companion.CONNECT_TIMEOUT
import app.filemanager.service.BaseSocketManager.Companion.SEND_IDENTIFIER
import app.filemanager.service.BaseSocketManager.Companion.SEND_LENGTH
import app.filemanager.service.data.SocketDevice
import app.filemanager.ui.state.main.DeviceState
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.sockets.Socket
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlin.coroutines.CoroutineContext

class JvmSocketServer(private val coroutineContext: CoroutineContext = Dispatchers.IO) : SocketServer {
    private val deviceState by inject<DeviceState>()
    private val settings = createSettings()
    private val devices = mutableMapOf<String, Pair<SocketDevice, ByteWriteChannel>>()
    private val database by inject<FileManagerDatabase>()

    private val socketDeviceHandler = SocketDeviceHandler()

    // TODO 捕获断开的信息
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun start(port: Int, callback: (clientId: String, message: SocketMessage) -> Unit) {
        withContext(coroutineContext) {
            val selectorManager = SelectorManager(Dispatchers.IO)
            val serverSocket = aSocket(selectorManager).tcp().bind("0.0.0.0", 1204)
            println("Server is listening at ${serverSocket.localAddress}")
            while (true) {
                val socket = serverSocket.accept()
                println("Accepted ${socket.remoteAddress}")
                launch {
                    val receiveChannel = socket.openReadChannel()
                    val sendChannel = socket.openWriteChannel(autoFlush = true)

                    val buffer = ByteArray(SEND_LENGTH)
                    while (true) {
                        val bufferLength = receiveChannel.readAvailable(buffer)
                        if (bufferLength == -1) {
                            break
                        }

                        try {
                            if (bufferLength < SEND_LENGTH) {
                                val socketMessage =
                                    ProtoBuf.decodeFromByteArray<SocketMessage>(buffer.copyOf(bufferLength))
                                if (socketMessage.header.command == "connect") {
                                    socketDeviceHandler.handleSocketDevice(socketMessage, sendChannel, socket)
                                } else {
                                    callback(socket.remoteAddress.toString(), socketMessage)
                                }
                            } else {
                                val index = findIdentifierIndex(buffer)
                                val socketMessage =
                                    ProtoBuf.decodeFromByteArray<SocketMessage>(buffer.copyOf(if (index == -1) bufferLength else index))
                                println("【server】 header = ${socketMessage.header} params = ${socketMessage.params} it=${if (index == -1) bufferLength else index}")
                                callback(socket.remoteAddress.toString(), socketMessage)
                            }
                        } catch (e: Exception) {
                            println("Error handling connection: ${e.message}")
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
        val byteArray = data + SEND_IDENTIFIER + ByteArray(SEND_LENGTH - (data.size + SEND_IDENTIFIER.size))
        deviceSendChannel.second.writeFully(byteArray)
    }
}


actual fun createSocketServer(): SocketServer {
    return JvmSocketServer()
}


class JvmSocketClient(private val coroutineContext: CoroutineContext = Dispatchers.IO) : SocketClient {
    private val settings = createSettings()
    private lateinit var selectorManager: SelectorManager
    private lateinit var socket: Socket

    private var sendChannel: ByteWriteChannel? = null

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun connect(host: String, port: Int, receive: (data: SocketMessage) -> Unit) {
        withContext(coroutineContext) {
            selectorManager = SelectorManager(Dispatchers.IO)
            socket = aSocket(selectorManager).tcp().connect(host, port)

            val receiveChannel = socket.openReadChannel()
            sendChannel = socket.openWriteChannel(autoFlush = true)
            sendChannel?.writeFully(
                ProtoBuf.encodeToByteArray(
                    SocketMessage.sendDevice(
                        deviceId = settings.getString("deviceId", ""),
                        deviceName = settings.getString("deviceName", ""),
                        host = socket.localAddress.toString(),
                        type = DeviceType.JVM
                    )
                )
            )


            val buffer = ByteArray(SEND_LENGTH)
            while (true) {
                val bufferLength = receiveChannel.readAvailable(buffer)
                if (bufferLength == -1) {
                    break
                }
                val index = findIdentifierIndex(buffer)
                val socketMessage =
                    ProtoBuf.decodeFromByteArray<SocketMessage>(buffer.copyOf(if (index == -1) bufferLength else index))
                println("【client】 header = ${socketMessage.header} params = ${socketMessage.params} it=${if (index == -1) bufferLength else index}")
                receive(socketMessage)
            }
        }
    }

    // 发送消息的方法
    override suspend fun send(clientId: String, data: ByteArray) {
        val byteArray = data + SEND_IDENTIFIER + ByteArray(SEND_LENGTH - (data.size + SEND_IDENTIFIER.size))
        sendChannel?.writeFully(byteArray)
    }

    override fun disconnect(): Boolean {
        return try {
            selectorManager.close()
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getAllIPAddresses(): List<String> {
        val networkAddresses = withContext(Dispatchers.IO) {
            NetworkInterface.getNetworkInterfaces()
        }
            .asSequence()
            .flatMap { netInterface ->
                netInterface.inetAddresses.asSequence()
                    .filter {
                        !it.isLoopbackAddress && (it is Inet4Address/* || it is Inet6Address*/)
                    }
                    .map { addr -> netInterface.displayName to addr.hostAddress }
            }
            .groupBy { it.first }

        return networkAddresses.map { it.value.map { pair -> pair.second } }.flatten()
    }

    override suspend fun scanner(address: List<String>, callback: (SocketDevice) -> Unit) {
        val ipAddresses = mutableSetOf<String>().apply {
            for (host in address) {
                addAll(host.getSubnetIps()/*.filter { it != host }*/)
            }
        }

        var length = ipAddresses.size

        val mainScope = MainScope()
        for (ips in ipAddresses.chunked(51)) {
            mainScope.launch {
                for (ip in ips) {
                    val socketDevice = scanPort(ip, 1204)
                    if (socketDevice != null) {
                        callback(socketDevice)
                    }
                    length--
                }
            }
        }

        while (length > 0) {
            delay(300L)
        }
    }


    @OptIn(ExperimentalSerializationApi::class)
    suspend fun scanPort(host: String, port: Int): SocketDevice? {
        val selectorManager = SelectorManager(Dispatchers.IO)
        return try {
            val socket = withTimeout(500L) {
                aSocket(selectorManager).tcp().connect(host, port)
            }
            withTimeout(800L) {
                val receiveChannel = socket.openReadChannel()
                val buffer = ByteArray(SEND_LENGTH)

                launch {
                    socket.openWriteChannel(autoFlush = true).writeFully(SEND_IDENTIFIER)
                }

                val bufferLength = receiveChannel.readAvailable(buffer)
                if (bufferLength == -1) {
                    throw Exception()
                }
                val socketMessage =
                    ProtoBuf.decodeFromByteArray<SocketMessage>(buffer.copyOf(bufferLength))
                if (socketMessage.body.isEmpty()) {
                    throw Exception()
                }

                ProtoBuf.decodeFromByteArray<SocketDevice>(socketMessage.body).apply {
                    this.host = host
                }
            }
        } catch (e: Exception) {
            null
        } finally {
            withContext(Dispatchers.IO) {
                selectorManager.close()
            }
        }
    }
}

actual fun createSocketClient(): SocketClient {
    return JvmSocketClient()
}


@OptIn(ExperimentalSerializationApi::class)
class SocketDeviceHandler : KoinComponent {
    private val deviceState by inject<DeviceState>()
    private val settings = createSettings()
    private val devices = mutableMapOf<String, Pair<SocketDevice, ByteWriteChannel>>()
    private val database by inject<FileManagerDatabase>()

    suspend fun handleSocketDevice(
        socketMessage: SocketMessage,
        sendChannel: ByteWriteChannel,
        socket: Socket
    ) = coroutineScope {
        val socketDevice = ProtoBuf.decodeFromByteArray<SocketDevice>(socketMessage.body)
        val queriedDevice = database.deviceQueries.queryById(socketDevice.id).executeAsOneOrNull()

        if (queriedDevice != null) {
            when (queriedDevice.connectionType) {
                PERMANENTLY_BANNED -> sendResponse(sendChannel, byteArrayOf())
                AUTO_CONNECT -> {
                    devices[socket.remoteAddress.toString()] = socketDevice to sendChannel
                    sendDeviceResponse(sendChannel, socket)
                }

                else -> {}
            }
        } else {
            handleFirstConnection(socketDevice, sendChannel, socket)
        }
    }

    private suspend fun handleFirstConnection(
        socketDevice: SocketDevice,
        sendChannel: ByteWriteChannel,
        socket: Socket
    ) {
        coroutineScope {
            deviceState.connectionRequest[socketDevice.id] = WAITING
            launch {
                try {
                    withTimeout(CONNECT_TIMEOUT * 1000L) {
                        while (deviceState.connectionRequest[socketDevice.id] == WAITING) {
                            delay(300L)
                        }
                        processConnectionRequest(socketDevice, sendChannel, socket)
                    }
                } catch (e: Exception) {
                    sendResponse(sendChannel, byteArrayOf())
                    deviceState.connectionRequest.remove(socketDevice.id)
                }
            }
        }
    }

    private suspend fun processConnectionRequest(
        socketDevice: SocketDevice,
        sendChannel: ByteWriteChannel,
        socket: Socket
    ) {
        when (deviceState.connectionRequest[socketDevice.id]) {
            AUTO_CONNECT, PERMANENTLY_BANNED -> {
                database.deviceQueries.insert(
                    id = socketDevice.id,
                    name = socketDevice.name,
                    type = socketDevice.type,
                    connectionType = deviceState.connectionRequest[socketDevice.id]!!
                )
                if (deviceState.connectionRequest[socketDevice.id] == PERMANENTLY_BANNED) {
                    throw Exception()
                }
            }

            APPROVED -> {}
            else -> throw Exception()
        }

        devices[socket.remoteAddress.toString()] = socketDevice to sendChannel
        sendDeviceResponse(sendChannel, socket)
    }

    private suspend fun sendResponse(sendChannel: ByteWriteChannel, body: ByteArray) {
        sendChannel.writeFully(
            ProtoBuf.encodeToByteArray(
                SocketMessage(
                    header = SocketHeader(command = "connect", devices = emptyList()),
                    params = emptyMap(),
                    body = body
                )
            )
        )
    }

    private suspend fun sendDeviceResponse(sendChannel: ByteWriteChannel, socket: Socket) {
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
    }
}