package app.filemanager.service.rpc

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.db.FileManagerDatabase
import app.filemanager.extensions.pathLevel
import app.filemanager.getSocketDevice
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.RpcClientManager.Companion.MAX_LENGTH
import app.filemanager.ui.state.file.FileShareStatus
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.utils.FileUtils
import io.ktor.client.*
import io.ktor.util.logging.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.protobuf.protobuf
import kotlinx.rpc.krpc.streamScoped
import kotlinx.rpc.withService
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RpcShareClientManager : KoinComponent {
    internal lateinit var shareService: ShareService
    internal var token: String = ""

    private val rpcClient by lazy { HttpClient { installKrpc() } }
    val fileState: FileState by inject()
    val deviceState: DeviceState by inject()
    val database: FileManagerDatabase by inject()

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun share(connectDevice: SocketDevice) {
        val ktorRpcClient = rpcClient.rpc {
            url {
                host = connectDevice.host.replace("[", "").replace("]", "")
                port = connectDevice.port
            }
            rpcConfig {
                serialization {
                    protobuf()
                }
            }
        }
        shareService = ktorRpcClient.withService<ShareService>()
        connect(this, connectDevice)
    }

    fun disconnect(): Boolean {
        rpcClient.cancel()
        rpcClient.close()
        return true
    }

    private suspend fun connect(manager: RpcShareClientManager, device: SocketDevice) {
        try {
            val share = shareService.connect(getSocketDevice())
            when (share.first) {
                DeviceConnectType.AUTO_CONNECT, DeviceConnectType.APPROVED -> {
                    val deviceShare = device.toShare(this)
                    deviceState.shares.add(deviceShare)
                    token = share.second

                    val deviceReceiveShare =
                        database.deviceReceiveShareQueries.selectById(device.id).executeAsOneOrNull()
                    // TODO 添加任务
                    if (deviceReceiveShare?.connectionType == DeviceConnectType.AUTO_CONNECT) {
                        FileUtils.getFile(deviceReceiveShare.path).onSuccess { destFileSimpleInfo ->
                            val files = mutableListOf<FileSimpleInfo>()
                            getList("/", "") { result ->
                                if (result.isSuccess) {
                                    files.addAll(result.getOrDefault(listOf()))
                                }
                            }

                            for (file in files) {
                                copyFile(file, destFileSimpleInfo) {
                                    println(it)
                                }
                            }
                        }

                        return
                    }

                    MainScope().launch {
                        testSpeed()
                    }

                    fileState.updateDesk(FileProtocol.Share, deviceShare)
                }

                DeviceConnectType.PERMANENTLY_BANNED, DeviceConnectType.REJECTED -> {}
                DeviceConnectType.WAITING -> {}
            }
        } catch (e: Exception) {
            println(e)
            println("Exception while connecting device: ${device.id}")
            KtorSimpleLogger(ShareService::class.simpleName!!).error(e.toString())
        }
    }

    /**
     * 从远程设备获取指定路径下的文件和文件夹列表。
     *
     * @param path 要获取列表的路径。
     * @param remoteId 远程设备的ID。
     */
    suspend fun getList(path: String, remoteId: String, replyCallback: (Result<List<FileSimpleInfo>>) -> Unit) {
        val fileSimpleInfos: MutableList<FileSimpleInfo> = mutableListOf()

        val result = shareService.list(token, path)
        if (!result.isSuccess) {
            replyCallback(Result.failure(result.deSerializable()))
            return
        }

        result.value?.forEach {
            it.value.forEach { fileSimpleInfo ->
                fileSimpleInfos.add(fileSimpleInfo.apply {
                    protocol = it.key.first
                    protocolId = it.key.second
                    this.path = path + this.path
                })
            }
        }
        replyCallback(Result.success(fileSimpleInfos))
    }

    suspend fun copyFile(
        srcFileSimpleInfo: FileSimpleInfo,
        destFileSimpleInfo: FileSimpleInfo,
        replyCallback: (Result<Boolean>) -> Unit
    ) {
        println("copyFile: $srcFileSimpleInfo -> $destFileSimpleInfo")
        val mainScope = MainScope()
        var successCount = 0
        var failureCount = 0

        // 只复制一个文件
        if (!srcFileSimpleInfo.isDirectory) {
            writeBytes(
                srcFileSimpleInfo,
                destFileSimpleInfo,
                srcFileSimpleInfo
            ) {
                database.shareHistoryQueries.insert(
                    fileName = srcFileSimpleInfo.name,
                    filePath = srcFileSimpleInfo.path,
                    fileSize = srcFileSimpleInfo.size,
                    isDirectory = srcFileSimpleInfo.isDirectory,
                    sourceDeviceId = srcFileSimpleInfo.protocolId,
                    targetDeviceId = getSocketDevice().id,
                    isOutgoing = false,
                    status = if (it.isSuccess) FileShareStatus.COMPLETED else FileShareStatus.ERROR,
                    errorMessage = "",
                    savePath = destFileSimpleInfo.path
                )
                replyCallback(it)
            }
            return
        }

        val list = mutableListOf<FileSimpleInfo>()

        streamScoped {
            shareService.traversePath(token, srcFileSimpleInfo.path).collect { fileAndFolder ->
                if (fileAndFolder.isSuccess) {
                    fileAndFolder.value?.forEach {
                        list.addAll(it.value)
                    }
                }
            }
        }

        if (FileUtils.createFolder(destFileSimpleInfo.path).isFailure) {
            replyCallback(Result.failure(Exception("文件夹创建失败")))
            return
        }

        list.sortedWith(
            compareBy<FileSimpleInfo> { !it.isDirectory }
                .thenBy { it.path.pathLevel() })
            .groupBy { it.isDirectory }.forEach { (isDir, fileSimpleInfos) ->
                if (isDir) {
                    for (paths in fileSimpleInfos.map { "${destFileSimpleInfo.path}${it.path}" }.chunked(30)) {
                        paths.forEach {
                            FileUtils.createFolder(it)
                                .onSuccess { success -> if (success) successCount++ else failureCount++ }
                                .onFailure { failureCount++ }
                        }
                    }
                } else {
                    for (files in fileSimpleInfos.chunked(maxOf(30, fileSimpleInfos.size / 30))) {
                        mainScope.launch {
                            for (file in files) {
                                writeBytes(
                                    srcFileSimpleInfo,
                                    destFileSimpleInfo,
                                    file
                                ) {
                                    if (it.getOrNull() == true) {
                                        successCount++
                                    } else {
                                        println(file.path)
                                        failureCount++
                                    }
                                }
                            }
                        }
                    }
                }
            }



        while (successCount + failureCount < list.size) {
            delay(100L)
        }

        val isSuccess = successCount + failureCount == list.size

        database.shareHistoryQueries.insert(
            fileName = srcFileSimpleInfo.name,
            filePath = srcFileSimpleInfo.path,
            fileSize = srcFileSimpleInfo.size,
            isDirectory = srcFileSimpleInfo.isDirectory,
            sourceDeviceId = srcFileSimpleInfo.protocolId,
            targetDeviceId = getSocketDevice().id,
            isOutgoing = false,
            status = if (isSuccess) FileShareStatus.COMPLETED else FileShareStatus.ERROR,
            errorMessage = "",
            savePath = destFileSimpleInfo.path
        )

        replyCallback(Result.success(isSuccess))
    }

    suspend fun writeBytes(
        srcFileSimpleInfo: FileSimpleInfo,
        destFileSimpleInfo: FileSimpleInfo,
        fileSimpleInfo: FileSimpleInfo,
        replyCallback: (Result<Boolean>) -> Unit
    ) {
        val srcFileSimpleInfoPath =
            if (srcFileSimpleInfo.isDirectory)
                "${srcFileSimpleInfo.path}${fileSimpleInfo.path}"
            else
                srcFileSimpleInfo.path

        val destFileSimpleInfoPath =
            if (srcFileSimpleInfo.isDirectory)
                "${destFileSimpleInfo.path}${fileSimpleInfo.path}"
            else
                destFileSimpleInfo.path

        if (fileSimpleInfo.size == 0L) {
            FileUtils.createFile(destFileSimpleInfoPath)
                .onSuccess { success -> replyCallback(Result.success(success)) }
                .onFailure { failure -> replyCallback(Result.failure(failure)) }
            return
        }

        var isSuccess = true
        streamScoped {
            shareService.readFileChunks(
                token,
                srcFileSimpleInfoPath,
                MAX_LENGTH.toLong()
            ).collect { result ->
                if (result.isSuccess && result.value != null) {
                    val writeBytes = FileUtils.writeBytes(
                        path = destFileSimpleInfoPath,
                        fileSize = fileSimpleInfo.size,
                        data = result.value.second,
                        offset = result.value.first * MAX_LENGTH
                    )
                    if (writeBytes.isFailure) {
                        isSuccess = false
                    }
                } else {
                    isSuccess = false
                }
            }
        }
        replyCallback(Result.success(isSuccess))
    }

    suspend fun testSpeed(){
        shareService.testSpeed(10000000).collect {
            println(it)
        }
    }
}