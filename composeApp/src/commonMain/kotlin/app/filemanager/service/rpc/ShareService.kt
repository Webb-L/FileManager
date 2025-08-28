package app.filemanager.service.rpc

import app.filemanager.createSettings
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.data.main.DeviceConnectType.APPROVED
import app.filemanager.data.main.DeviceConnectType.REJECTED
import app.filemanager.exception.AuthorityException
import app.filemanager.exception.EmptyDataException
import app.filemanager.exception.ParameterErrorException
import app.filemanager.exception.toSocketResult
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.extensions.parsePath
import app.filemanager.extensions.randomString
import app.filemanager.extensions.replaceLast
import app.filemanager.service.WebSocketResult
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.HttpRouteClientManager.Companion.MAX_LENGTH
import app.filemanager.ui.state.device.DeviceCertificateState
import app.filemanager.ui.state.file.FileShareState
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.rpc.annotations.Rpc
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.random.Random

@Rpc
interface ShareService {
    /**
     * 获取指定路径下的文件列表
     * @param token 设备认证令牌
     * @param path 要列出的文件路径
     * @return WebSocketResult包含文件列表的映射，键为协议和设备ID对，值为文件信息列表
     */
    suspend fun list(
        token: String,
        path: String
    ): WebSocketResult<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>

    /**
     * 遍历指定路径下的文件目录结构
     * @param token 设备认证令牌
     * @param path 要遍历的起始路径
     * @return Flow流式返回WebSocketResult，包含遍历过程中的文件列表
     */
    fun traversePath(
        token: String,
        path: String
    ): Flow<WebSocketResult<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>>

    /**
     * 分块读取文件内容
     * @param token 设备认证令牌
     * @param path 要读取的文件路径
     * @param chunkSize 每个数据块的大小(字节)
     * @return Flow流式返回WebSocketResult，包含文件块数据和偏移量
     */
    fun readFileChunks(
        token: String,
        path: String,
        chunkSize: Long
    ): Flow<WebSocketResult<Pair<Long, ByteArray>>>

    /**
     * 连接设备并建立共享会话
     * @param device 要连接的设备信息
     * @return Pair包含连接状态和认证令牌(如果连接成功)
     */
    suspend fun connect(device: SocketDevice): Pair<DeviceConnectType, String>

    fun testSpeed(count: Int): Flow<ByteArray>
}

/**
 * ShareService接口的实现类，提供文件共享相关功能
 *
 * @constructor 创建文件共享服务实例
 */
class ShareServiceImpl() : ShareService, KoinComponent {
    private val settings = createSettings()
    private val fileState: FileState by inject()
    private val deviceState: DeviceState by inject()
    private val fileShareState: FileShareState by inject()
    private val deviceCertificateState: DeviceCertificateState by inject()

    /**
     * 实现获取文件列表功能
     * 1. 验证token和路径有效性
     * 2. 检查隐藏文件访问权限
     * 3. 返回格式化后的文件列表
     *
     * @param token 设备认证令牌
     * @param path 要列出的文件路径
     * @return WebSocketResult包含文件列表
     */
    override suspend fun list(
        token: String,
        path: String
    ): WebSocketResult<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>> {
        val validationResult: Pair<Boolean, List<FileSimpleInfo>>
        try {
            validationResult = validateShareAndGetFiles(token, path)
        } catch (e: Exception) {
            return e.toSocketResult()
        }

        val fileSimpleInfoList = validationResult.second
            .filter { file -> if (validationResult.first) true else !file.isHidden }

        if (path == "/") {
            return WebSocketResult(
                value = mutableMapOf<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>().apply {
                    fileSimpleInfoList
                        .forEach { fileSimpleInfo ->
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

        // 检查路径中的每个段落，判断是否包含隐藏文件
        val parentFileSimpleInfo: FileSimpleInfo
        try {
            parentFileSimpleInfo = validatePathAndCheckHiddenFileAccess(
                path,
                fileSimpleInfoList.find { path.indexOf("/${it.name}") == 0 }
                    ?: return ParameterErrorException().toSocketResult(),
                validationResult,
            )
        } catch (e: Exception) {
            return e.toSocketResult()
        }

        val fileAndFolder = parentFileSimpleInfo.path.getFileAndFolder()
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
                        this.path = this.path.replace(parentFileSimpleInfo.path, "")
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

    /**
     * 实现目录遍历功能
     * 1. 验证token和路径有效性
     * 2. 使用PathUtils遍历目录结构
     * 3. 通过Flow流式返回遍历结果
     *
     * @param token 设备认证令牌
     * @param path 要遍历的起始路径
     * @return Flow流式返回遍历结果
     */
    override fun traversePath(
        token: String,
        path: String
    ): Flow<WebSocketResult<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>> {
        var isFirst = false
        return channelFlow {

            val validationResult: Pair<Boolean, List<FileSimpleInfo>>;
            try {
                validationResult = validateShareAndGetFiles(token, path)
            } catch (e: Exception) {
                send(e.toSocketResult())
                return@channelFlow
            }

            val fileSimpleInfoList = validationResult.second
                .filter { file -> if (validationResult.first) true else !file.isHidden }

            val rootFileSimpleInfo = fileSimpleInfoList.find { path.indexOf("/${it.name}") == 0 }
            if (rootFileSimpleInfo == null) {
                send(ParameterErrorException().toSocketResult())
                return@channelFlow
            }

            // 检查路径中的每个段落，判断是否包含隐藏文件
            val parentFileSimpleInfo: FileSimpleInfo
            try {
                parentFileSimpleInfo = validatePathAndCheckHiddenFileAccess(path, rootFileSimpleInfo, validationResult)
            } catch (e: Exception) {
                send(e.toSocketResult())
                return@channelFlow
            }

            PathUtils.traverse(parentFileSimpleInfo.path) { fileAndFolder ->
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
                                files.add(FileUtils.getFile(parentFileSimpleInfo.path).getOrNull()!!)
                                isFirst = true
                            }
                            files.forEach { fileSimpleInfo ->
                                val key = if (fileSimpleInfo.protocol == FileProtocol.Local)
                                    Pair(FileProtocol.Device, settings.getString("deviceId", ""))
                                else
                                    Pair(fileSimpleInfo.protocol, fileSimpleInfo.protocolId)

                                if (!containsKey(key)) {
                                    put(key, mutableListOf(fileSimpleInfo.apply {
                                        this.path = this.path.replace(parentFileSimpleInfo.path, "")
                                        this.protocol = FileProtocol.Local
                                        this.protocolId = ""
                                    }))
                                } else {
                                    get(key)?.add(fileSimpleInfo.apply {
                                        this.path = this.path.replace(parentFileSimpleInfo.path, "")
                                        this.protocol = FileProtocol.Local
                                        this.protocolId = ""
                                    })
                                }
                            }
                        })
                    }

                    launch { send(result) }
                } catch (e: Exception) {
                    println(e)
                }
            }
        }
    }

    /**
     * 实现文件分块读取功能
     * 1. 验证token和路径有效性
     * 2. 使用FileUtils分块读取文件
     * 3. 通过Flow流式返回文件块数据
     *
     * @param token 设备认证令牌
     * @param path 要读取的文件路径
     * @param chunkSize 每个数据块的大小(字节)
     * @return Flow流式返回文件块数据
     */
    override fun readFileChunks(
        token: String,
        path: String,
        chunkSize: Long
    ): Flow<WebSocketResult<Pair<Long, ByteArray>>> {
        return channelFlow {

            val validationResult: Pair<Boolean, List<FileSimpleInfo>>
            try {
                validationResult = validateShareAndGetFiles(token, path)
            } catch (e: Exception) {
                println("$e, $path")
                send(e.toSocketResult())
                return@channelFlow
            }

            val fileSimpleInfoList = validationResult.second
                .filter { file -> if (validationResult.first) true else !file.isHidden }

            val rootFileSimpleInfo = fileSimpleInfoList.find { path.indexOf("/${it.name}") == 0 }
            if (rootFileSimpleInfo == null) {
                send(ParameterErrorException().toSocketResult())
                return@channelFlow
            }

            // 检查路径中的每个段落，判断是否包含隐藏文件
            val fileSimpleInfo: FileSimpleInfo
            try {
                fileSimpleInfo = validatePathAndCheckHiddenFileAccess(path, rootFileSimpleInfo, validationResult)
            } catch (e: Exception) {
                send(e.toSocketResult())
                return@channelFlow
            }

            FileUtils.readFileChunks(fileSimpleInfo.path, chunkSize) { result ->
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

    /**
     * 实现设备连接功能
     * 1. 检查设备是否在允许连接列表中
     * 2. 生成随机token用于后续认证
     * 3. 返回连接状态和token
     *
     * @param device 要连接的设备信息
     * @return Pair包含连接状态和认证令牌
     */
    override suspend fun connect(device: SocketDevice): Pair<DeviceConnectType, String> {
        return if (deviceState.allowDeviceShareConnection.contains(device.id)) {
            val token = 32.randomString()
            fileShareState.deviceToken[token] = device.id
            Pair(APPROVED, token)
        } else {
            Pair(REJECTED, "")
        }
    }

    /**
     * 验证共享文件的权限并获取已共享文件列表
     *
     * @param token 设备令牌标识
     * @param path 文件路径
     * @return 如果验证通过，返回共享文件列表；否则返回错误结果
     */
    /**
     * 验证共享权限并获取共享文件列表
     * @param token 设备认证令牌
     * @param path 要访问的文件路径
     * @return Pair包含是否有权限访问隐藏文件和共享文件列表
     * @throws ParameterErrorException 当token无效或路径为空时抛出
     */
    private fun validateShareAndGetFiles(token: String, path: String): Pair<Boolean, List<FileSimpleInfo>> {
        val deviceId = fileShareState.deviceToken[token] ?: throw ParameterErrorException()
        val sharedFiles =
            fileShareState.shareToDevices[deviceId] ?: throw ParameterErrorException()

        if (path.isEmpty()) throw ParameterErrorException()

        return sharedFiles
    }

    /**
     * 验证文件路径并检查访问权限
     *
     * @param relativePath 相对路径
     * @param parentFile 父文件信息
     * @param accessPermission 访问权限验证结果
     * @return 验证通过的文件信息
     * @throws ParameterErrorException 当文件不存在时抛出
     * @throws AuthorityException 当尝试访问无权限的隐藏文件或文件夹时抛出
     */
    /**
     * 验证文件路径并检查隐藏文件访问权限
     * 1. 检查文件是否存在
     * 2. 验证中间路径中的隐藏文件访问权限
     *
     * @param relativePath 相对路径
     * @param parentFile 父文件信息
     * @param accessPermission 访问权限验证结果
     * @return 验证通过的文件信息
     * @throws ParameterErrorException 当文件不存在时抛出
     * @throws AuthorityException 当尝试访问无权限的隐藏文件时抛出
     */
    private fun validatePathAndCheckHiddenFileAccess(
        relativePath: String,
        parentFile: FileSimpleInfo,
        accessPermission: Pair<Boolean, Any>
    ): FileSimpleInfo {
        // 检查文件是否隐藏
        val absolutePath = "${parentFile.path.replaceLast("/${parentFile.name}", "")}$relativePath"
        val fileInfo =
            FileUtils.getFile(absolutePath).getOrNull() ?: throw ParameterErrorException()
        if (!accessPermission.first && fileInfo.isHidden) throw AuthorityException("对方拒绝访问隐藏文件或文件夹")

        val pathSegments = relativePath.parsePath()

        if (pathSegments.indices.any { index ->
                val intermediatePathToCheck = parentFile.path.replaceLast(
                    "/${parentFile.name}",
                    "/" + pathSegments.subList(0, index + 1).joinToString(PathUtils.getPathSeparator())
                )
                val intermediateFileInfo = FileUtils.getFile(intermediatePathToCheck).getOrNull()
                intermediateFileInfo != null && !accessPermission.first && intermediateFileInfo.isHidden
            }) {
            throw AuthorityException("对方拒绝访问隐藏文件或文件夹")
        }

        return fileInfo
    }


    override fun testSpeed(count: Int): Flow<ByteArray> {
        return channelFlow {
            repeat(count) {
                send(Random.nextBytes(MAX_LENGTH))
                println("send $it")
            }
        }
    }
}
