package app.filemanager.ui.state.main

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceCategory
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.data.main.Share
import app.filemanager.db.FileManagerDatabase
import app.filemanager.extensions.getSubnetIps
import app.filemanager.getSocketDevice
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.RpcClientManager
import app.filemanager.service.rpc.RpcClientManager.Companion.PORT
import app.filemanager.service.rpc.RpcShareClientManager
import app.filemanager.ui.state.file.FileShareState
import app.filemanager.ui.state.file.FileShareStatus
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.logging.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeviceState : KoinComponent {
    private val database by inject<FileManagerDatabase>()
    private val mainState by inject<MainState>()
    private val fileShareState = inject<FileShareState>()
    private val mainScope = MainScope()
    private val client = HttpClient {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 500
            connectTimeoutMillis = 500
            socketTimeoutMillis = 500
        }
    }

    private val _isDeviceAdd: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isDeviceAdd: StateFlow<Boolean> = _isDeviceAdd
    fun updateDeviceAdd(value: Boolean) {
        _isDeviceAdd.value = value
    }

    val devices = mutableStateListOf<Device>()

    val socketDevices = mutableStateListOf<SocketDevice>()

    private val _loadingDevices: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val loadingDevices: StateFlow<Boolean> = _loadingDevices
    fun updateLoadingDevices(value: Boolean) {
        _loadingDevices.value = value
    }

    // Map<设备id, Pair<设备链接类型, 结束倒计时>>
    val connectionRequest = mutableStateMapOf<String, Pair<DeviceConnectType, Long>>()

    suspend fun scanner(address: List<String>, port: Int = PORT) {
        val path = "${PathUtils.getCachePath()}${PathUtils.getPathSeparator()}scanner_server_cache"
        val readFile = FileUtils.readFile(path)
        val scannerIps = mutableSetOf<String>()
        updateLoadingDevices(true)
        val ipAddresses = mutableSetOf<String>().apply {
            if (readFile.isSuccess) {
                val fileContent = readFile.getOrDefault(byteArrayOf())
                addAll(readFile.getOrDefault(byteArrayOf()).decodeToString(0, fileContent.size).split(","))
            }
            for (host in address) {
                addAll(host.getSubnetIps().filter { it != host })
            }
        }

        var remainingAddresses = ipAddresses.size

        ipAddresses.chunked(51).forEach { chunk ->
            mainScope.launch(Dispatchers.Default) {
                chunk.forEach { ip ->
                    try {
                        pingDevice(ip, port)
                        scannerIps.add(ip)
                    } catch (e: Exception) {
                        socketDevices.removeAll { it.host == ip }
                    } finally {
                        remainingAddresses--
                    }
                }
            }
        }

        while (remainingAddresses > 0) {
            delay(300L)
        }
        val toByteArray = scannerIps.joinToString(",").toByteArray()
        FileUtils.writeBytes(
            path,
            fileSize = toByteArray.size.toLong(),
            data = toByteArray,
            offset = 0
        )

        updateLoadingDevices(false)
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun pingDevice(ip: String, port: Int) {
        val response = client.get {
            url {
                host = ip
                path("/ping")
                this.port = port
            }
        }

        val socketDevice = if (response.bodyAsBytes().isNotEmpty()) {
            ProtoBuf.decodeFromByteArray<SocketDevice>(response.bodyAsBytes())
        } else null

        socketDevice?.let { device ->
            device.host = ip

            val deviceData = database.deviceQueries.queryById(device.id).executeAsOneOrNull()
            if (deviceData == null) {
                database.deviceQueries.insert(
                    id = device.id,
                    name = device.name,
                    host = device.host,
                    port = device.port.toLong(),
                    type = device.type
                )
            } else {
                if ((deviceData.name != device.name && deviceData.hasRemarks == false) || deviceData.type != device.type) {
                    database.deviceQueries.updateNameAndTypeById(device.name, device.type, device.id)
                } else {
                    device.name = deviceData.name
                }
            }

            // 自动连接
            database.deviceConnectQueries.queryByIdAndCategory(device.id, DeviceCategory.CLIENT).executeAsOneOrNull()
                .let {
                    if (it == null) {
                        device.connectType = ConnectType.New
                    }
                    if (it?.connectionType == DeviceConnectType.AUTO_CONNECT) {
                        device.connectType = ConnectType.Loading
                        connect(device)
                    }
                }

            if (socketDevices.firstOrNull { it.id == device.id } == null) {
                socketDevices.add(device)
            }
        }
    }

    fun connect(connectDevice: SocketDevice) {
        mainScope.launch(Dispatchers.Default) {
            try {
                val rpcClientManager = RpcClientManager()
                rpcClientManager.connect(connectDevice)
            } catch (e: Exception) {
                println(e)
            }
        }
    }


    val shares = mutableStateListOf<Share>()

    // Map<设备id, Pair<设备链接类型, 结束倒计时>>
    val shareRequest = mutableStateMapOf<String, Pair<DeviceConnectType, Long>>()

    // 允许远程设备连接分享服务
    val allowDeviceShareConnection = mutableSetOf<String>()

    // 设置连接状态 Map<设备Id， 设备连接类型>
    val shareConnectionStates = mutableStateMapOf<String, FileShareStatus>()

    @OptIn(ExperimentalSerializationApi::class)
    fun share(device: SocketDevice) {
        val scope = MainScope()
        scope.launch(Dispatchers.Default) {
            try {
                withContext(Dispatchers.Default) {
                    val socketDevice = ProtoBuf.encodeToHexString(getSocketDevice())
                    val httpClient = HttpClient { install(SSE) }
                    httpClient.sse("http://${device.host}:${device.port}/share/$socketDevice") {
                        incoming.collect { event ->
                            KtorSimpleLogger("DeviceState").info(event.toString())
                            val fileShareStatus = FileShareStatus.valueOf(event.event ?: "")
                            when (fileShareStatus) {
                                FileShareStatus.SENDING -> {}
                                FileShareStatus.REJECTED -> {}
                                FileShareStatus.ERROR -> {}
                                FileShareStatus.COMPLETED -> {
                                    allowDeviceShareConnection.add(device.id)
                                }

                                FileShareStatus.WAITING -> {
                                    allowDeviceShareConnection.add(device.id)
                                    fileShareState.value.shareToDevices[device.id]?.second?.forEach { fileSimpleInfo ->
                                        database.shareHistoryQueries.insert(
                                            fileName = fileSimpleInfo.name,
                                            filePath = fileSimpleInfo.path,
                                            fileSize = fileSimpleInfo.size,
                                            isDirectory = fileSimpleInfo.isDirectory,
                                            sourceDeviceId = getSocketDevice().id,
                                            targetDeviceId = device.id,
                                            isOutgoing = true,
                                            status = fileShareStatus,
                                            errorMessage = "",
                                            savePath = ""
                                        )
                                    }
                                }
                            }
                            fileShareState.value.sendFile[device.id] = fileShareStatus
                        }
                    }
                }
            } catch (e: Exception) {
                println("Request failed: ${e.message}")
                fileShareState.value.sendFile[device.id] = FileShareStatus.ERROR
            } finally {
                // 关闭 HttpClient
                client.close()
                scope.cancel()
            }
        }
    }


    fun connectShare(device: SocketDevice) {
        mainScope.launch(Dispatchers.Default) {
            try {
                withContext(Dispatchers.Default) {
                    val rpcClientManager = RpcShareClientManager()
                    rpcClientManager.share(device)
                }
            } catch (e: Exception) {
                println(e)
            }
        }
    }
}