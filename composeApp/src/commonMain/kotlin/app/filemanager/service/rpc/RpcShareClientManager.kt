package app.filemanager.service.rpc

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.extensions.pathLevel
import app.filemanager.getSocketDevice
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.RpcClientManager.Companion.MAX_LENGTH
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
                    fileState.updateDesk(FileProtocol.Share, deviceShare)
                }

                DeviceConnectType.PERMANENTLY_BANNED, DeviceConnectType.REJECTED -> {}
                DeviceConnectType.WAITING -> {}
            }
        } catch (e: Exception) {
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
//        if (!srcFileSimpleInfo.isDirectory) {
//            fileHandle.writeBytes(
//                remoteId,
//                srcFileSimpleInfo,
//                destFileSimpleInfo,
//                srcFileSimpleInfo
//            ) {
//                replyCallback(it)
//            }
//            return
//        }

        val list = mutableListOf<FileSimpleInfo>()

        // 获取远程所有的文件和文件夹
//        var fileService: FileService = rpc.fileService
//        val socketDevice =
//            deviceState.shares.firstOrNull { it.id == destFileSimpleInfo.protocolId }
//        if (socketDevice == null) {
//            replyCallback(Result.failure(Exception("设备离线")))
//            return
//        }
//        fileService = socketDevice.rpcClientManager

        // 只复制文件或文件夹
//        if (srcFileSimpleInfo.size == 0L) {
//            val result = if (srcFileSimpleInfo.isDirectory)
//                fileService.createFolder(rpc.token, listOf(destFileSimpleInfo.path))
//            else
//                fileService.createFile(rpc.token, listOf(destFileSimpleInfo.path))
//
//            if (result.isSuccess) {
//                replyCallback(Result.success(result.value?.first()?.value == true))
//            } else {
//                replyCallback(Result.failure(result.deSerializable()))
//            }
//            return
//        }
//
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

        replyCallback(Result.success(successCount + failureCount == list.size))
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
                "rpc.token",
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
}