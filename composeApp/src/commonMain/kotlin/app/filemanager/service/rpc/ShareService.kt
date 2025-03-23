package app.filemanager.service.rpc

import app.filemanager.createSettings
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.data.main.DeviceConnectType.APPROVED
import app.filemanager.data.main.DeviceConnectType.REJECTED
import app.filemanager.exception.EmptyDataException
import app.filemanager.exception.ParameterErrorException
import app.filemanager.exception.toSocketResult
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.extensions.replaceLast
import app.filemanager.service.WebSocketResult
import app.filemanager.service.data.SocketDevice
import app.filemanager.ui.state.device.DeviceCertificateState
import app.filemanager.ui.state.file.FileShareState
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.CoroutineContext

@Rpc
interface ShareService : RemoteService {
    // 远程设备需要我本地文件
    // TODO 检查权限
    suspend fun list(
        token: String,
        path: String
    ): WebSocketResult<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>

    // 发送遍历的目录
    // TODO 检查权限
    suspend fun traversePath(
        token: String,
        path: String
    ): Flow<WebSocketResult<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>>

    // 读取文件数据
    // TODO 检查权限
    suspend fun readFileChunks(
        token: String,
        path: String,
        chunkSize: Long
    ): Flow<WebSocketResult<Pair<Long, ByteArray>>>

    // 获取文件信息
    // TODO 检查权限
//    suspend fun getFileByPath(token: String, path: String): WebSocketResult<FileSimpleInfo>

    // 获取文件信息
    // TODO 检查权限
//    suspend fun getFileByPathAndName(token: String, path: String, name: String): WebSocketResult<FileSimpleInfo>

    suspend fun connect(device: SocketDevice): Pair<DeviceConnectType, String>
}

class ShareServiceImpl(override val coroutineContext: CoroutineContext) : ShareService, KoinComponent {
    private val settings = createSettings()
    private val fileState: FileState by inject()
    private val deviceState: DeviceState by inject()
    private val fileShareState: FileShareState by inject()
    private val deviceCertificateState: DeviceCertificateState by inject()

    // TODO 检查是否有权限
    override suspend fun list(
        token: String,
        path: String
    ): WebSocketResult<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>> {
//        if (deviceCertificateState.checkPermission(
//                token,
//                path,
//                "read"
//            )
//        ) {
//            return AuthorityException("对方没有为你设置权限").toSocketResult()
//        }

        if (path.isEmpty()) {
            return ParameterErrorException().toSocketResult()
        }

        if (path == "/") {
            return WebSocketResult(
                value = mutableMapOf<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>().apply {
                    fileShareState.checkedFiles.forEach { fileSimpleInfo ->
                        val key = if (fileSimpleInfo.protocol == FileProtocol.Local)
                            Pair(FileProtocol.Share, settings.getString("deviceId", ""))
                        else
                            Pair(fileSimpleInfo.protocol, fileSimpleInfo.protocolId)

                        val simpleInfo = fileSimpleInfo.withCopy(
                            path = fileSimpleInfo.name,
                            protocol = FileProtocol.Local,
                            protocolId = ""
                        )

                        if (!containsKey(key)) {
                            put(key, mutableListOf(simpleInfo))
                        } else {
                            get(key)?.add(simpleInfo)
                        }
                    }
                }
            )
        }


        val fileSimpleInfo = fileShareState.checkedFiles.find { path.indexOf("/${it.name}") == 0 }
            ?: return ParameterErrorException().toSocketResult()

        val parentPath = "${fileSimpleInfo.path.replaceLast("/${fileSimpleInfo.name}", "")}$path"

        val fileAndFolder = parentPath.getFileAndFolder()

        if (fileAndFolder.isFailure) {
            val exceptionOrNull = fileAndFolder.exceptionOrNull() ?: EmptyDataException()
            return WebSocketResult(
                exceptionOrNull.message,
                exceptionOrNull::class.simpleName,
                null
            )
        }

        return WebSocketResult(
            value = mutableMapOf<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>().apply {
                fileAndFolder.getOrNull()?.forEach { fileSimpleInfo ->
                    val key = if (fileSimpleInfo.protocol == FileProtocol.Local)
                        Pair(FileProtocol.Share, settings.getString("deviceId", ""))
                    else
                        Pair(fileSimpleInfo.protocol, fileSimpleInfo.protocolId)


                    val simpleInfo = fileSimpleInfo.apply {
                        this.path = this.path.replace(parentPath, "")
                        this.protocol = FileProtocol.Local
                        this.protocolId = ""
                    }
                    if (!containsKey(key)) {
                        put(key, mutableListOf(simpleInfo))
                    } else {
                        get(key)?.add(simpleInfo)
                    }
                }
            }
        )
    }

    override suspend fun traversePath(
        token: String,
        path: String
    ): Flow<WebSocketResult<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>> {
        var isFirst = false
        return channelFlow {
//            if (deviceCertificateState.checkPermission(
//                    token,
//                    path,
//                    "read"
//                )
//            ) {
//                send(AuthorityException("对方没有为你设置权限").toSocketResult())
//                cancel()
//                return@channelFlow
//            }

            if (path.isEmpty()) {
                launch {
                    send(EmptyDataException().toSocketResult())
                }
                return@channelFlow
            }

            // TODO 不应该使用 fileShareState.checkedFiles
            val fileSimpleInfo = fileShareState.checkedFiles.find { path.indexOf("/${it.name}") == 0 }
            if (fileSimpleInfo==null) {
                send(ParameterErrorException().toSocketResult())
                return@channelFlow
            }

            val parentPath = "${fileSimpleInfo.path.replaceLast("/${fileSimpleInfo.name}", "")}$path"

            PathUtils.traverse(parentPath) { fileAndFolder ->
                try {
                    val result = if (fileAndFolder.isFailure) {
                        val exceptionOrNull = fileAndFolder.exceptionOrNull() ?: EmptyDataException()
                        WebSocketResult(
                            exceptionOrNull.message,
                            exceptionOrNull::class.simpleName,
                            null
                        )
                    } else {
                        WebSocketResult(value = mutableMapOf<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>().apply {
                            val files: MutableList<FileSimpleInfo> = mutableListOf()
                            files.addAll(fileAndFolder.getOrDefault(listOf()))
                            if (!isFirst) {
                                files.add(FileUtils.getFile(parentPath).getOrNull()!!)
                                isFirst = true
                            }
                            files.forEach { fileSimpleInfo ->
                                val key = if (fileSimpleInfo.protocol == FileProtocol.Local)
                                    Pair(FileProtocol.Device, settings.getString("deviceId", ""))
                                else
                                    Pair(fileSimpleInfo.protocol, fileSimpleInfo.protocolId)

                                if (!containsKey(key)) {
                                    put(key, mutableListOf(fileSimpleInfo.apply {
                                        this.path = this.path.replace(parentPath, "")
                                        this.protocol = FileProtocol.Local
                                        this.protocolId = ""
                                    }))
                                } else {
                                    get(key)?.add(fileSimpleInfo.apply {
                                        this.path = this.path.replace(parentPath, "")
                                        this.protocol = FileProtocol.Local
                                        this.protocolId = ""
                                    })
                                }
                            }
                        })
                    }

                    launch {
                        send(result)
                    }
                } catch (e: Exception) {
                    println(e)
                }
            }
        }
    }

    override suspend fun readFileChunks(
        token: String,
        path: String,
        chunkSize: Long
    ): Flow<WebSocketResult<Pair<Long, ByteArray>>> {
        return channelFlow {
//            if (deviceCertificateState.checkPermission(
//                    token,
//                    path,
//                    "read"
//                )
//            ) {
//                send(AuthorityException("对方没有为你设置权限").toSocketResult())
//                cancel()
//                return@channelFlow
//            }

            if (path.isEmpty()) {
                launch {
                    send(EmptyDataException().toSocketResult())
                }
                return@channelFlow
            }

            val fileSimpleInfo = fileShareState.checkedFiles.find { path.indexOf("/${it.name}") == 0 }
            if (fileSimpleInfo==null) {
                send(ParameterErrorException().toSocketResult())
                return@channelFlow
            }

            val parentPath = "${fileSimpleInfo.path.replaceLast("/${fileSimpleInfo.name}", "")}$path"
            FileUtils.readFileChunks(parentPath, chunkSize) { result ->
                val sendData = if (result.isSuccess) {
                    WebSocketResult(value = result.getOrDefault(Pair(0L, byteArrayOf())))
                } else {
                    val exceptionOrNull = result.exceptionOrNull() ?: EmptyDataException()
                    WebSocketResult(
                        exceptionOrNull.message,
                        exceptionOrNull::class.simpleName,
                        null
                    )
                }
                launch {
                    send(sendData)
                }
            }
        }
    }

    override suspend fun connect(device: SocketDevice): Pair<DeviceConnectType, String> {
        // 检查是否有权限。
        return if (deviceState.allowDeviceShareConnection.contains(device.id)) {
            Pair(APPROVED, "")
        } else {
            Pair(REJECTED, "")
        }
    }
}