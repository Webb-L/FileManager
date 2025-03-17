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

    override suspend fun connect(device: SocketDevice): Pair<DeviceConnectType, String> {
        // 检查是否有权限。
        return if (deviceState.allowDeviceShareConnection.contains(device.id)) {
            Pair(APPROVED, "")
        }else {
            Pair(REJECTED, "")
        }
    }
}