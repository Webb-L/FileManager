package app.filemanager.service.rpc

import app.filemanager.createSettings
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.PathInfo
import app.filemanager.exception.EmptyDataException
import app.filemanager.exception.ParameterErrorException
import app.filemanager.exception.toSocketResult
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.service.WebSocketResult
import app.filemanager.ui.state.file.FileState
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
interface PathService : RemoteService {
    // 远程设备需要我本地文件
    // TODO 检查权限
    suspend fun list(path: String): WebSocketResult<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>

    // 远程设备需要我本地的书签
    // TODO 检查权限
    suspend fun rootPaths(): WebSocketResult<List<PathInfo>>

    // 发送遍历的目录
    // TODO 检查权限
    suspend fun traversePath(path: String): Flow<WebSocketResult<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>>
}

class PathServiceImpl(override val coroutineContext: CoroutineContext) : PathService, KoinComponent {
    private val settings = createSettings()
    private val fileState: FileState by inject()

    override suspend fun list(path: String): WebSocketResult<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>> {
        if (path.isEmpty()) {
            return ParameterErrorException().toSocketResult()
        }

        val fileAndFolder = path.getFileAndFolder()

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
                        Pair(FileProtocol.Device, settings.getString("deviceId", ""))
                    else
                        Pair(fileSimpleInfo.protocol, fileSimpleInfo.protocolId)

                    if (!containsKey(key)) {
                        put(key, mutableListOf(fileSimpleInfo.apply {
                            this.path = this.path.replace(path, "")
                            this.protocol = FileProtocol.Local
                            this.protocolId = ""
                        }))
                    } else {
                        get(key)?.add(fileSimpleInfo.apply {
                            this.path = this.path.replace(path, "")
                            this.protocol = FileProtocol.Local
                            this.protocolId = ""
                        })
                    }
                }
            }
        )
    }

    override suspend fun rootPaths(): WebSocketResult<List<PathInfo>> {
        val rootPaths = PathUtils.getRootPaths()
        if (rootPaths.isFailure) {
            val exceptionOrNull = rootPaths.exceptionOrNull() ?: EmptyDataException()
            return WebSocketResult(
                exceptionOrNull.message,
                exceptionOrNull::class.simpleName,
                null
            )
        }

        return WebSocketResult(value = rootPaths.getOrDefault(listOf()))
    }

    override suspend fun traversePath(
        path: String
    ): Flow<WebSocketResult<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>> {
        var isFirst = false
        return channelFlow {
            PathUtils.traverse(path) { fileAndFolder ->
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
                            files.add(FileUtils.getFile(path).getOrNull()!!)
                            isFirst = true
                        }
                        files.forEach { fileSimpleInfo ->
                            val key = if (fileSimpleInfo.protocol == FileProtocol.Local)
                                Pair(FileProtocol.Device, settings.getString("deviceId", ""))
                            else
                                Pair(fileSimpleInfo.protocol, fileSimpleInfo.protocolId)

                            if (!containsKey(key)) {
                                put(key, mutableListOf(fileSimpleInfo.apply {
                                    this.path = this.path.replace(path, "")
                                    this.protocol = FileProtocol.Local
                                    this.protocolId = ""
                                }))
                            } else {
                                get(key)?.add(fileSimpleInfo.apply {
                                    this.path = this.path.replace(path, "")
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
            }
        }
    }
}