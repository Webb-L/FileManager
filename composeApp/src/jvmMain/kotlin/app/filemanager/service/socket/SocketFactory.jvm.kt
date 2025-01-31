package app.filemanager.service.socket

import app.filemanager.createSettings
import app.filemanager.data.main.DeviceCategory
import app.filemanager.data.main.DeviceConnectType.*
import app.filemanager.data.main.DeviceType
import app.filemanager.db.FileManagerDatabase
import app.filemanager.extensions.getSubnetIps
import app.filemanager.service.BaseSocketManager.Companion.CONNECT_TIMEOUT
import app.filemanager.service.BaseSocketManager.Companion.PORT
import app.filemanager.service.BaseSocketManager.Companion.SEND_IDENTIFIER
import app.filemanager.service.BaseSocketManager.Companion.SEND_LENGTH
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.SocketDevice
import app.filemanager.ui.state.main.DeviceState
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.sockets.Socket
import io.ktor.util.network.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.component.inject
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import kotlin.coroutines.CoroutineContext

class JvmSocketServer(private val coroutineContext: CoroutineContext = Dispatchers.IO) : SocketServer {
    private val deviceState by inject<DeviceState>()
    private val settings = createSettings()
    private val devices = mutableMapOf<String, Pair<SocketDevice, ByteWriteChannel>>()
    private val database by inject<FileManagerDatabase>()
    private val mainScope = MainScope()

    // TODO 捕获断开的信息
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun start(port: Int, callback: (clientId: String, message: SocketMessage) -> Unit) {
        withContext(coroutineContext) {
            val selectorManager = SelectorManager(Dispatchers.IO)
            val serverSocket = aSocket(selectorManager).tcp().bind("0.0.0.0", PORT)
            println("Server is listening at ${serverSocket.localAddress}")
            while (true) {
                val socket = serverSocket.accept()
                val remoteAddress = socket.remoteAddress.toString()
                println("Accepted ${socket.remoteAddress}")
                launch {
                    val receiveChannel = socket.openReadChannel()
                    val sendChannel = socket.openWriteChannel(autoFlush = true)

                    val buffer = ByteArray(SEND_LENGTH)
                    while (true) {
                        val bufferLength = receiveChannel.readAvailable(buffer)
                        // 设备离线
                        if (bufferLength == -1) {
                            devices.remove(remoteAddress)
                            break
                        }

                        try {
                            if (bufferLength < SEND_LENGTH) {
                                val socketMessage =
                                    ProtoBuf.decodeFromByteArray<SocketMessage>(buffer.copyOf(bufferLength))
                                if (socketMessage.header.command == "connect") {
                                    handleSocketDevice(socketMessage, sendChannel, socket)
                                } else {
                                    callback(remoteAddress, socketMessage)
                                }
                            } else {
                                val index = findIdentifierIndex(buffer)
                                val socketMessage =
                                    ProtoBuf.decodeFromByteArray<SocketMessage>(buffer.copyOf(if (index == -1) bufferLength else index))
                                println("【server】 header = ${socketMessage.header} params = ${socketMessage.params} it=${if (index == -1) bufferLength else index}")
                                callback(remoteAddress, socketMessage)
                            }
                        } catch (e: Exception) {
                            println("Error handling connection: ${e.message}")
                            sendDeviceResponse(sendChannel, socket)
                            socket.close()
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun handleSocketDevice(
        socketMessage: SocketMessage,
        sendChannel: ByteWriteChannel,
        socket: Socket
    ) {
        mainScope.launch {
            val socketDevice = ProtoBuf.decodeFromByteArray<SocketDevice>(socketMessage.body)
            val queriedDevice =
                database.deviceQueries.queryById(socketDevice.id, DeviceCategory.SERVER).executeAsOneOrNull()

            if (queriedDevice != null) {
                println(queriedDevice)
                when (queriedDevice.connectionType) {
                    PERMANENTLY_BANNED,
                    AUTO_CONNECT -> {
                        if (queriedDevice.connectionType == PERMANENTLY_BANNED) {
                            sendResponse(sendChannel, byteArrayOf())
                        }
                        if (queriedDevice.connectionType == AUTO_CONNECT) {
                            devices[socket.remoteAddress.toString()] = socketDevice to sendChannel
                            sendDeviceResponse(sendChannel, socket)
                        }
                        database.deviceQueries.updateConnectionTypeByIdAndCategory(
                            connectionType = queriedDevice.connectionType,
                            id = socketDevice.id,
                            category = DeviceCategory.SERVER
                        )
                    }

                    APPROVED, REJECTED -> {
                        deviceState.connectionRequest[socketDevice.id] = WAITING
                        try {
                            withTimeout(CONNECT_TIMEOUT * 1000L) {
                                while (deviceState.connectionRequest[socketDevice.id] == WAITING) {
                                    delay(300L)
                                }
                                when (deviceState.connectionRequest[socketDevice.id]) {
                                    AUTO_CONNECT, APPROVED -> {}
                                    else -> throw Exception()
                                }

                                devices[socket.remoteAddress.toString()] = socketDevice to sendChannel
                                sendDeviceResponse(sendChannel, socket)
                            }
                        } catch (e: Exception) {
                            println(e)
                            sendResponse(sendChannel, byteArrayOf())
                            deviceState.connectionRequest.remove(socketDevice.id)
                        }
                    }

                    else -> {}
                }
                database.deviceQueries.updateLastConnectionByCategoryAndId(socketDevice.id, DeviceCategory.SERVER)
            } else {
                deviceState.connectionRequest[socketDevice.id] = WAITING
                try {
                    withTimeout(CONNECT_TIMEOUT * 1000L) {
                        while (deviceState.connectionRequest[socketDevice.id] == WAITING) {
                            delay(300L)
                        }
                        database.deviceQueries.insert(
                            id = socketDevice.id,
                            name = socketDevice.name,
                            type = socketDevice.type,
                            connectionType = deviceState.connectionRequest[socketDevice.id] ?: WAITING,
                            category = DeviceCategory.SERVER
                        )
                        when (deviceState.connectionRequest[socketDevice.id]) {
                            AUTO_CONNECT, APPROVED -> {}
                            else -> throw Exception()
                        }

                        devices[socket.remoteAddress.toString()] = socketDevice to sendChannel
                        sendDeviceResponse(sendChannel, socket)
                    }
                } catch (e: Exception) {
                    sendResponse(sendChannel, byteArrayOf())
                    deviceState.connectionRequest.remove(socketDevice.id)
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
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

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun sendDeviceResponse(sendChannel: ByteWriteChannel, socket: Socket) {
        sendChannel.writeFully(
            ProtoBuf.encodeToByteArray(
                SocketMessage.sendDevice(
                    deviceId = settings.getString("deviceId", ""),
                    deviceName = settings.getString("deviceName", ""),
                    host = socket.localAddress.toJavaAddress().address,
                    type = DeviceType.JVM
                )
            )
        )
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
    private val database by inject<FileManagerDatabase>()


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
                        host = socket.localAddress.toJavaAddress().address,
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

    override fun getAllIPAddresses(type: SocketClientIPEnum): List<String> {
        val addresses = mutableListOf<String>()

        val interfaces = NetworkInterface.getNetworkInterfaces()
        interfaces.iterator().forEach { networkInterface ->
            if (type == SocketClientIPEnum.IPV4_UP && (!networkInterface.isUp || networkInterface.isLoopback)) return@forEach
            // 获取该网络接口下所有的 InetAddress
            networkInterface.inetAddresses.iterator().forEach { inetAddress ->
                when (inetAddress) {
                    is Inet4Address -> addresses.add(inetAddress.hostAddress)
                    is Inet6Address -> {
                        if (type == SocketClientIPEnum.ALL) {
                            addresses.add("[${inetAddress.hostAddress.replace("%.*$".toRegex(), "")}]")
                        }
                    }
                }
            }
        }

        if (type == SocketClientIPEnum.ALL) {
            addresses.addAll(listOf("[::1]", "[0:0:0:0:0:0:0:0]"))
        }

        return addresses
    }

    override suspend fun scanner(address: List<String>, callback: (SocketDevice) -> Unit) {
        val ipAddresses = mutableSetOf<String>().apply {
            for (host in address) {
                addAll(host.getSubnetIps().filter { it != host })
            }
        }

        var length = ipAddresses.size

        val mainScope = MainScope()
        for (ips in ipAddresses.chunked(51)) {
            mainScope.launch {
                for (ip in ips) {
                    val socketDevice = scanPort(ip, PORT)
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
    override suspend fun scanPort(host: String, port: Int): SocketDevice? {
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
                    val deviceConfig =
                        database.deviceQueries.queryById(this.id, DeviceCategory.CLIENT).executeAsOneOrNull()
                    if (deviceConfig != null) {
                        if (deviceConfig.connectionType == AUTO_CONNECT) {
                            this.connectType = ConnectType.Loading
                        } else {
                            this.connectType = ConnectType.UnConnect
                        }
                    } else {
                        this.connectType = ConnectType.New
                    }
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