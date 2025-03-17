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
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeviceState : KoinComponent {
    private val database by inject<FileManagerDatabase>()
    private val mainState by inject<MainState>()
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
                addAll(host.getSubnetIps()/*.filter { it != host }*/)
            }
        }

        var remainingAddresses = ipAddresses.size

        ipAddresses.chunked(51).forEach { chunk ->
            mainScope.launch {
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

            // 自动连接
            database.deviceQueries.queryById(device.id, DeviceCategory.CLIENT).executeAsOneOrNull().let {
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
        mainScope.launch {
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
    @OptIn(InternalAPI::class, ExperimentalSerializationApi::class)
    fun share(device: SocketDevice) {
        mainScope.launch {
            try {
                // 发起 POST 请求
                val response: HttpResponse = withContext(Dispatchers.Default) {
                    HttpClient().post("http://${device.host}:${device.port}/share") {
                        contentType(ContentType.Application.OctetStream)
                        this.body = ProtoBuf.encodeToByteArray(getSocketDevice())
                    }
                }

                // TODO 请求成功
                if (response.status == HttpStatusCode.OK) {
                    allowDeviceShareConnection.add(device.id)
                }
                println("Response status: ${response.status}")
                println("Response body: ${response.bodyAsText()}")
            } catch (e: Exception) {
                println("Request failed: ${e.message}")
            } finally {
                // 关闭 HttpClient
                client.close()
            }
        }
    }


    fun connectShare(device: SocketDevice) {
        mainScope.launch {
            try {
                val rpcClientManager = RpcShareClientManager()
                rpcClientManager.share(device)
            } catch (e: Exception) {
                println(e)
            }
        }
    }
}