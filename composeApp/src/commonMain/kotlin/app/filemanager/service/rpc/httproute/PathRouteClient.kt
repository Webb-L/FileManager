package app.filemanager.service.rpc.httproute

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.PathInfo
import app.filemanager.extensions.pathLevel
import app.filemanager.service.data.*
import app.filemanager.service.rpc.HttpRouteClientManager
import app.filemanager.service.rpc.HttpRouteClientManager.Companion.MAX_LENGTH
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.ui.state.main.Task
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.ceil

class PathRouteClient(
    private val httpClient: HttpClient,
    private val manager: HttpRouteClientManager
) : KoinComponent {
    private val deviceState: DeviceState by inject()

    suspend fun getRootPaths(): Result<List<PathInfo>> {
        return try {
            val response = httpClient.post("/api/paths/rootPaths") {
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun listPath(request: ListRequest): Result<List<FileSimpleInfo>> {
        return try {
            val fileSimpleInfos: MutableList<FileSimpleInfo> = mutableListOf()

            val response = httpClient.post("/api/paths/list") {
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            val responseBody =
                response.body<SerializableResult<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>>()
                    .toResult()
            if (responseBody.isFailure) {
                throw responseBody.exceptionOrNull()!!
            }
            responseBody.getOrDefault(mapOf()).forEach { (protocol, fileInfos) ->
                for (info in fileInfos) {
                    fileSimpleInfos.add(info.apply {
                        this.protocol = protocol.first
                        this.protocolId = protocol.second
                        this.path = request.path + this.path
                    })
                }
            }

            Result.success(fileSimpleInfos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    @OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
    fun traversePath(request: TraversePathRequest): Flow<Result<List<FileSimpleInfo>>> {
        return flow {
            try {
                val response = httpClient.post("/api/paths/traverse") {
                    setBody(request)
                }

                if (!response.status.isSuccess()) {
                    throw Exception(response.bodyAsText())
                }

                val responseText = response.bodyAsText()
                val lines = responseText.split("\n")

                for (line in lines) {
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data.isNotEmpty()) {
                            try {
                                val fileSimpleInfos: MutableList<FileSimpleInfo> = mutableListOf()

                                val decodedBytes = Base64.decode(data)
                                val responseBody = ProtoBuf.decodeFromByteArray(
                                    kotlinx.serialization.serializer<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>(),
                                    decodedBytes
                                )
                                responseBody.forEach { (protocol, fileInfos) ->
                                    for (info in fileInfos) {
                                        fileSimpleInfos.add(info.apply {
                                            this.protocol = protocol.first
                                            this.protocolId = protocol.second
                                            this.path = request.path + this.path
                                        })
                                    }
                                }

                                emit(Result.success(fileSimpleInfos))
                            } catch (e: Exception) {
                                emit(Result.failure(e))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
        }
    }

    suspend fun copyFile(
        task: Task,
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
            task.values["progressMax"] = "1"
            writeBytes(
                srcFileSimpleInfo,
                destFileSimpleInfo,
                srcFileSimpleInfo
            ) {
                task.values["progressCur"] = (successCount + failureCount).toString()
                replyCallback(it)
            }
            return
        }

        val list = mutableListOf<FileSimpleInfo>()


        // 获取本地所有的文件和文件夹
        if (srcFileSimpleInfo.protocol == FileProtocol.Local) {
            task.values["progressMax"] = "1"
            // 只有一个文件夹或文件
            if (srcFileSimpleInfo.size == 0L) {
                val result = if (srcFileSimpleInfo.isDirectory)
                    FileUtils.createFolder(destFileSimpleInfo.path)
                else
                    FileUtils.createFile(destFileSimpleInfo.path)

                result
                    .onSuccess { success ->
                        task.values["progressCur"] = "1"
                        replyCallback(Result.success(success))
                    }
                    .onFailure { failure ->
                        task.result[destFileSimpleInfo.path] = failure.message.orEmpty()
                        replyCallback(Result.failure(failure))
                    }
                return
            }

            // 获取所有文件和文件夹
            PathUtils.traverse(srcFileSimpleInfo.path) { fileAndFolder ->
                if (fileAndFolder.isSuccess) {
                    list.addAll(fileAndFolder.getOrDefault(listOf()).map {
                        it.path = it.path.replaceFirst(srcFileSimpleInfo.path, "")
                        it
                    })
                }
            }
            task.values["progressMax"] = list.size.toString()
        }

        // 获取远程所有的文件和文件夹
        if (srcFileSimpleInfo.protocol == FileProtocol.Device) {
            var fileService: FileRouteClient = manager.fileRouteClient
            if (destFileSimpleInfo.protocol == FileProtocol.Device) {
                val socketDevice =
                    deviceState.socketDevices.firstOrNull { device -> device.id == destFileSimpleInfo.protocolId && device.httpClient != null }
                if (socketDevice == null) {
                    replyCallback(Result.failure(Exception("设备离线")))
                    return
                }
                fileService = socketDevice.httpClient!!.fileRouteClient
            }

            // 只复制文件或文件夹
            if (srcFileSimpleInfo.size == 0L) {
                task.values["progressMax"] = "1"
                val result = if (srcFileSimpleInfo.isDirectory)
                    fileService.createFolders(
                        listOf(
                            CreateInfo(
                                destFileSimpleInfo.path,
                                destFileSimpleInfo.name,
                            )
                        )
                    )
                else
                    fileService.createFiles(
                        listOf(
                            CreateInfo(
                                destFileSimpleInfo.path,
                                destFileSimpleInfo.name,
                            )
                        )
                    )

                task.values["progressCur"] = "1"
                result
                    .onSuccess { results ->
                        if (results.isNotEmpty()) {
                            replyCallback(results.first())
                        } else {
                            replyCallback(Result.success(false))
                        }
                    }
                    .onFailure {
                        task.result[destFileSimpleInfo.path] = result.exceptionOrNull()?.message.orEmpty()
                        replyCallback(Result.failure(it))
                    }
                return
            }

            manager.pathRouteClient.traversePath(TraversePathRequest(srcFileSimpleInfo.path))
                .collect { fileAndFolder ->
                    if (fileAndFolder.isSuccess) {
                        fileAndFolder.onSuccess { files ->
                            list.addAll(files)
                        }
                    }
                }
            task.values["progressMax"] = list.size.toString()
        }

        if (destFileSimpleInfo.isDirectory) {
            if (destFileSimpleInfo.protocol == FileProtocol.Local) {
                val createFolder = FileUtils.createFolder(destFileSimpleInfo.path)
                task.values["progressCur"] = "1"
                if (createFolder.isFailure) {
                    task.result[destFileSimpleInfo.path] = createFolder.exceptionOrNull()?.message.orEmpty()
                    replyCallback(Result.failure(createFolder.exceptionOrNull() ?: Exception()))
                    return
                }
            }

            if (destFileSimpleInfo.protocol == FileProtocol.Device) {
                var fileService: FileRouteClient = manager.fileRouteClient
                if (srcFileSimpleInfo.protocol == FileProtocol.Device) {
                    val socketDevice =
                        deviceState.socketDevices
                            .firstOrNull { device -> device.id == destFileSimpleInfo.protocolId && device.httpClient != null }
                    if (socketDevice == null) {
                        replyCallback(Result.failure(Exception("设备离线")))
                        return
                    }
                    fileService = socketDevice.httpClient!!.fileRouteClient
                }

                val createFolder = fileService.createFolders(
                    listOf(
                        CreateInfo(
                            destFileSimpleInfo.path,
                            destFileSimpleInfo.name,
                        )
                    )
                )
                if (!createFolder.isSuccess) {
                    task.result[destFileSimpleInfo.path] = createFolder.exceptionOrNull()?.message.orEmpty()
                    replyCallback(Result.failure(createFolder.exceptionOrNull() ?: Exception()))
                    return
                } else {
                    val results = createFolder.getOrDefault(listOf())
                    if (results.isNotEmpty()) {
                        if (results.first().isSuccess == false) {
                            task.result[destFileSimpleInfo.path] = results.first().exceptionOrNull()?.message.orEmpty()
                            replyCallback(Result.failure(results.first().exceptionOrNull() ?: Exception()))
                            return
                        }
                    } else {
                        task.result[destFileSimpleInfo.path] = "创建失败"
                        replyCallback(Result.failure(Exception("创建失败")))
                        return
                    }
                }
            }
        }

        list.sortedWith(
            compareBy<FileSimpleInfo> { !it.isDirectory }
                .thenBy { it.path.pathLevel() })
            .groupBy { it.isDirectory }.forEach { (isDir, fileSimpleInfos) ->
                if (isDir) {
                    var fileService: FileRouteClient = manager.fileRouteClient
                    if (srcFileSimpleInfo.protocol == FileProtocol.Device && destFileSimpleInfo.protocol == FileProtocol.Device) {
                        val socketDevice =
                            deviceState.socketDevices
                                .firstOrNull { device -> device.id == destFileSimpleInfo.protocolId && device.httpClient != null }
                        if (socketDevice == null) {
                            replyCallback(Result.failure(Exception("设备离线")))
                            return
                        }
                        fileService = socketDevice.httpClient!!.fileRouteClient
                    }

                    for (paths in fileSimpleInfos.map {
                        CreateInfo(
                            destFileSimpleInfo.path,
                            it.path
                        )
                    }.chunked(30)) {
                        if (destFileSimpleInfo.protocol == FileProtocol.Local) {
                            paths.forEach { path ->
                                FileUtils.createFolder(path.join())
                                    .onSuccess { success -> if (success) successCount++ else failureCount++ }
                                    .onFailure {
                                        failureCount++
                                        task.values[path.join()] = it.message.orEmpty()
                                    }
                                task.values["progressCur"] = (successCount + failureCount).toString()
                            }
                        }

                        if (destFileSimpleInfo.protocol == FileProtocol.Device) {
                            val result = fileService.createFolders(paths)
                            if (result.isSuccess) {
                                result.getOrDefault(listOf()).forEach { item ->
                                    when {
                                        item.isSuccess && item.getOrDefault(false) -> successCount++
                                        else -> failureCount++
                                    }
                                    task.values["progressCur"] = (successCount + failureCount).toString()
                                }
                            } else {
                                failureCount += paths.size
                            }
                        }
                    }
                } else {
                    for (files in fileSimpleInfos.chunked(maxOf(30, fileSimpleInfos.size / 30))) {
                        mainScope.launch(Dispatchers.Default) {
                            for (file in files) {
                                manager.pathRouteClient.writeBytes(
                                    srcFileSimpleInfo,
                                    destFileSimpleInfo,
                                    file
                                ) { result ->
                                    if (result.getOrNull() == true) {
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

        var length = ceil(fileSimpleInfo.size / MAX_LENGTH.toFloat()).toLong()
        val mainScope = MainScope()

        // 本地 to 远程
        if (srcFileSimpleInfo.protocol == FileProtocol.Local && destFileSimpleInfo.protocol == FileProtocol.Device) {
            if (fileSimpleInfo.size == 0L) {
                println("destFileSimpleInfoPath = $destFileSimpleInfoPath, ${destFileSimpleInfo.path}, ${destFileSimpleInfo.name}")
                val createFileRequest = CreateFileRequest(
                    listOf(
                        CreateInfo(
                            destFileSimpleInfo.path,
                            destFileSimpleInfo.name,
                        )
                    )
                )
                manager.fileRouteClient
                    .createFiles(createFileRequest.infos)
                    .onSuccess {
                        if (it.isNotEmpty()) {
                            replyCallback(it.first())
                        } else {
                            replyCallback(Result.success(false))
                        }
                    }
                    .onFailure { replyCallback(Result.failure(it)) }
                return
            }

            var isSuccess = true
            FileUtils.readFileChunks(srcFileSimpleInfoPath, MAX_LENGTH.toLong()) { result ->
                if (result.isSuccess) {
                    val result = result.getOrNull() ?: Pair(0L, byteArrayOf())
                    mainScope.launch(Dispatchers.Default) {
                        manager.fileRouteClient.writeBytes(
                            fileSize = fileSimpleInfo.size,
                            blockIndex = result.first,
                            blockLength = length,
                            path = destFileSimpleInfoPath,
                            byteArray = result.second
                        )
                            .onSuccess {
                                length--
                            }
                            .onFailure {
                                isSuccess = false
                                // TODO 记录错误
                            }
                    }
                } else {
                    isSuccess = false
                    // TODO 记录错误
                    length--
                    replyCallback(Result.failure(result.exceptionOrNull() ?: Exception()))
                }
            }

            while (length > 0 || isSuccess) {
                delay(300)
            }

            replyCallback(Result.success(length == 0L && isSuccess))
            return
        }

        // 远程 to 本地
        if (srcFileSimpleInfo.protocol == FileProtocol.Device && destFileSimpleInfo.protocol == FileProtocol.Local) {
            if (fileSimpleInfo.size == 0L) {
                FileUtils.createFile(destFileSimpleInfoPath)
                    .onSuccess { success -> replyCallback(Result.success(success)) }
                    .onFailure { failure -> replyCallback(Result.failure(failure)) }
                return
            }

            var isSuccess = true
            repeat(ceil(srcFileSimpleInfo.size / MAX_LENGTH.toFloat()).toInt()) { index ->
                manager.fileRouteClient.readBytes(
                    srcFileSimpleInfoPath,
                    index.toLong(),
                    MAX_LENGTH.toLong()
                )
                    .onSuccess {
                        FileUtils.writeBytes(
                            path = destFileSimpleInfoPath,
                            fileSize = fileSimpleInfo.size,
                            data = it,
                            offset = index * MAX_LENGTH.toLong()
                        )
                    }
                    .onFailure {
                        isSuccess = false
                    }
            }
            replyCallback(Result.success(isSuccess))
            return
        }


        // 远程 to 远程
        if (srcFileSimpleInfo.protocol == FileProtocol.Device && destFileSimpleInfo.protocol == FileProtocol.Device) {
            var destFileService: FileRouteClient = manager.fileRouteClient
            if (srcFileSimpleInfo.protocol == FileProtocol.Device && destFileSimpleInfo.protocol == FileProtocol.Device) {
                val socketDevice =
                    deviceState.socketDevices.firstOrNull { device -> device.id == destFileSimpleInfo.protocolId && device.httpClient != null }
                if (socketDevice == null) {
                    replyCallback(Result.failure(Exception("设备离线")))
                    return
                }
                destFileService = socketDevice.httpClient!!.fileRouteClient
            }
            if (fileSimpleInfo.size == 0L) {
                println("destFileSimpleInfoPath = $destFileSimpleInfoPath, ${destFileSimpleInfo.path}, ${destFileSimpleInfo.name}")
                val createFileRequest = CreateFileRequest(
                    listOf(
                        CreateInfo(
                            destFileSimpleInfo.path,
                            destFileSimpleInfo.name,
                        )
                    )
                )
                destFileService.createFiles(createFileRequest.infos)
                    .onSuccess {
                        if (it.isNotEmpty()) {
                            replyCallback(it.first())
                        } else {
                            replyCallback(Result.success(false))
                        }
                    }
                    .onFailure { replyCallback(Result.failure(it)) }
                return
            }


            var isSuccess = true
            repeat(ceil(srcFileSimpleInfo.size / MAX_LENGTH.toFloat()).toInt()) { index ->
                manager.fileRouteClient.readBytes(
                    srcFileSimpleInfoPath,
                    index.toLong(),
                    MAX_LENGTH.toLong()
                )
                    .onSuccess {
                        destFileService.writeBytes(
                            fileSize = fileSimpleInfo.size,
                            blockIndex = index.toLong(),
                            blockLength = length,
                            path = destFileSimpleInfoPath,
                            byteArray = it
                        )
                            .onSuccess { }
                            .onFailure {
                                isSuccess = false
                            }
                    }
                    .onFailure {
                        isSuccess = false
                    }
            }
            replyCallback(Result.success(isSuccess))
            return
        }

        replyCallback(Result.success(false))
    }
}
