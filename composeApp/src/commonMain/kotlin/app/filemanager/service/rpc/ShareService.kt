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

/**
 * 文件共享服务RPC接口
 * 
 * 定义了远程设备访问共享文件所需的核心功能：
 * - 获取文件列表
 * - 递归遍历目录
 * - 分块读取文件
 * - 设备连接和认证
 */
@Rpc
interface ShareService : RemoteService {
    /**
     * 获取指定路径下的文件列表
     * 
     * @param token 设备认证令牌，用于验证访问权限
     * @param path 要列出文件的路径，以"/"开始的相对路径
     * @return WebSocketResult包含文件列表映射，键为协议和设备ID对，值为文件信息列表
     */
    suspend fun list(
        token: String,
        path: String
    ): WebSocketResult<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>

    /**
     * 递归遍历指定路径下的文件目录结构
     * 
     * @param token 设备认证令牌，用于验证访问权限
     * @param path 要遍历的起始路径
     * @return Flow流式返回WebSocketResult，包含遍历过程中的文件列表
     */
    suspend fun traversePath(
        token: String,
        path: String
    ): Flow<WebSocketResult<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>>

    /**
     * 分块读取文件内容，适用于大文件传输
     * 
     * @param token 设备认证令牌，用于验证访问权限
     * @param path 要读取的文件路径
     * @param chunkSize 每个数据块的大小(字节)
     * @return Flow流式返回WebSocketResult，包含文件块数据（偏移量和字节数组）
     */
    suspend fun readFileChunks(
        token: String,
        path: String,
        chunkSize: Long
    ): Flow<WebSocketResult<Pair<Long, ByteArray>>>

    /**
     * 连接设备并建立共享会话
     * 
     * @param device 要连接的设备信息，包含设备ID和其他标识信息
     * @return Pair包含连接状态(APPROVED/REJECTED)和认证令牌(如果连接成功)
     */
    suspend fun connect(device: SocketDevice): Pair<DeviceConnectType, String>
}

/**
 * ShareService接口的实现类，提供文件共享的具体功能实现
 * 
 * 主要职责：
 * - 验证设备权限和访问令牌
 * - 处理文件列表请求和内容读取
 * - 管理设备连接状态
 * - 处理隐藏文件的访问控制
 * 
 * @property coroutineContext 协程上下文，用于异步操作
 * @constructor 创建文件共享服务实例
 */
class ShareServiceImpl(override val coroutineContext: CoroutineContext) : ShareService, KoinComponent {
    /** 应用设置，存储设备ID等信息 */
    private val settings = createSettings()
    
    /** 文件状态管理 */
    private val fileState: FileState by inject()
    
    /** 设备状态管理 */
    private val deviceState: DeviceState by inject()
    
    /** 文件共享状态管理，包含共享文件列表和设备令牌 */
    private val fileShareState: FileShareState by inject()
    
    /** 设备证书状态管理 */
    private val deviceCertificateState: DeviceCertificateState by inject()

    /**
     * 获取指定路径下的文件列表
     * 
     * 实现步骤：
     * 1. 验证token和路径有效性
     * 2. 检查隐藏文件访问权限
     * 3. 处理根路径的特殊情况
     * 4. 格式化返回结果，调整文件路径和协议信息
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
     * 递归遍历指定路径下的文件目录结构
     * 
     * 实现步骤：
     * 1. 验证token和路径有效性
     * 2. 查找并验证目标目录
     * 3. 使用PathUtils递归遍历目录结构
     * 4. 通过channelFlow流式返回遍历结果
     * 
     * @param token 设备认证令牌
     * @param path 要遍历的起始路径
     * @return Flow流式返回遍历过程中的文件列表
     */
    override suspend fun traversePath(
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
     * 分块读取文件内容，适用于大文件传输
     * 
     * 实现步骤：
     * 1. 验证token和路径有效性
     * 2. 查找并验证目标文件
     * 3. 使用FileUtils分块读取文件内容
     * 4. 通过channelFlow流式返回文件块数据
     * 
     * @param token 设备认证令牌
     * @param path 要读取的文件路径
     * @param chunkSize 每个数据块的大小(字节)
     * @return Flow流式返回文件块数据和偏移量
     */
    override suspend fun readFileChunks(
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
     * 连接设备并建立共享会话
     * 
     * 实现步骤：
     * 1. 检查设备是否在允许连接列表中
     * 2. 如果允许连接，生成随机token并保存关联
     * 3. 返回连接状态和token（如果成功）
     * 
     * @param device 要连接的设备信息
     * @return Pair包含连接状态(APPROVED/REJECTED)和认证令牌(如果连接成功)
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
     * 验证共享权限并获取共享文件列表
     * 
     * 验证步骤：
     * 1. 根据token查找关联的设备ID
     * 2. 验证设备是否有共享文件权限
     * 3. 检查路径是否有效
     * 
     * @param token 设备认证令牌
     * @param path 要访问的文件路径
     * @return Pair包含是否有权限访问隐藏文件和共享文件列表
     * @throws ParameterErrorException 当token无效、设备未授权或路径为空时抛出
     */
    private fun validateShareAndGetFiles(token: String, path: String): Pair<Boolean, List<FileSimpleInfo>> {
        val deviceId = fileShareState.deviceToken[token] ?: throw ParameterErrorException()
        val sharedFiles =
            fileShareState.shareToDevices[deviceId] ?: throw ParameterErrorException()

        if (path.isEmpty()) throw ParameterErrorException()

        return sharedFiles
    }

    /**
     * 验证文件路径并检查隐藏文件访问权限
     * 
     * 验证步骤：
     * 1. 构建完整的文件路径并检查文件是否存在
     * 2. 检查目标文件是否为隐藏文件，并验证访问权限
     * 3. 逐段检查路径中的每个中间目录是否为隐藏目录
     * 
     * @param relativePath 相对路径
     * @param parentFile 父文件信息
     * @param accessPermission 访问权限验证结果，第一个元素表示是否可访问隐藏文件
     * @return 验证通过的文件信息
     * @throws ParameterErrorException 当文件不存在时抛出
     * @throws AuthorityException 当尝试访问无权限的隐藏文件或文件夹时抛出
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
}