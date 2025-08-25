package app.filemanager.service.routes

import app.filemanager.createSettings
import app.filemanager.data.file.FileProtocol
import app.filemanager.service.data.*
import app.filemanager.service.plugins.ProtobufRequest
import app.filemanager.service.plugins.receiveProtobuf
import app.filemanager.service.plugins.respondProtobuf
import app.filemanager.service.rpc.RpcClientManager.Companion.MAX_LENGTH
import app.filemanager.ui.state.device.DeviceCertificateState
import app.filemanager.utils.FileUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// 辅助函数：从 Authorization 头获取 token
fun ApplicationCall.getAuthToken(): String? {
    return request.headers["Authorization"]?.removePrefix("Bearer ")
}

@OptIn(ExperimentalSerializationApi::class)
fun Route.fileRoutes(): KoinComponent {
    val settings = createSettings()

    return object : KoinComponent {
        private val deviceCertificateState by inject<DeviceCertificateState>()

        init {
            route("/api/files") {
                install(ProtobufRequest)

                post("/rename") {
                    try {
                        val request = call.receiveProtobuf<RenameRequest>()

                        if (request.renameInfos.isEmpty()) {
                            call.respond(HttpStatusCode.BadRequest, "重命名信息不能为空")
                            return@post
                        }

                        val results = request.renameInfos.map { renameInfo ->
                            val token = call.getAuthToken()
                            if (token != null && deviceCertificateState.checkPermission(
                                    token,
                                    "${renameInfo.path}${app.filemanager.utils.PathUtils.getPathSeparator()}${renameInfo.oldName}",
                                    "rename"
                                )
                            ) {
                                return@map false
                            }

                            if (renameInfo.hasEmptyField()) {
                                return@map false
                            }

                            FileUtils.rename(renameInfo.path, renameInfo.oldName, renameInfo.newName)
                                .getOrDefault(false)
                        }
                        call.respondProtobuf(results)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "重命名失败: ${e.message}")
                    }
                }

                post("/create-folder") {
                    try {
                        val request = call.receiveProtobuf<CreateFolderRequest>()

                        if (request.names.isEmpty()) {
                            call.respond(HttpStatusCode.BadRequest, "文件夹名称不能为空")
                            return@post
                        }

                        val results = request.names.map { path ->
                            val token = call.getAuthToken()
                            if (token != null && deviceCertificateState.checkPermission(
                                    token,
                                    path,
                                    "write"
                                )
                            ) {
                                return@map false
                            }
                            FileUtils.createFolder(path).getOrDefault(false)
                        }

                        println(results)
                        call.respondProtobuf(results)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "创建文件夹失败: ${e.message}")
                    }
                }

                post("/size-info") {
                    try {
                        val request = call.receiveProtobuf<GetSizeInfoRequest>()

                        val token = call.getAuthToken()
                        if (token != null && deviceCertificateState.checkPermission(
                                token,
                                request.fileSimpleInfo.path,
                                "read"
                            )
                        ) {
                            call.respond(HttpStatusCode.Forbidden, "没有权限访问该路径")
                            return@post
                        }

                        if (request.totalSpace <= -1L || request.freeSpace <= -1) {
                            call.respond(HttpStatusCode.BadRequest, "存储空间参数无效")
                            return@post
                        }

                        val sizeInfo = request.fileSimpleInfo.getSizeInfo(request.totalSpace, request.freeSpace)
                        call.respondProtobuf(sizeInfo)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "获取大小信息失败: ${e.message}")
                    }
                }

                post("/delete") {
                    try {
                        val request = call.receiveProtobuf<DeleteRequest>()

                        if (request.names.isEmpty()) {
                            call.respond(HttpStatusCode.BadRequest, "删除路径不能为空")
                            return@post
                        }

                        val results = request.names.map { path ->
                            val token = call.getAuthToken()
                            if (token != null && deviceCertificateState.checkPermission(
                                    token,
                                    path,
                                    "remove"
                                )
                            ) {
                                return@map false
                            }

                            FileUtils.deleteFile(path).getOrDefault(false)
                        }
                        call.respondProtobuf(results)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "删除失败: ${e.message}")
                    }
                }

                post("/write-bytes") {
                    try {
                        val request = call.receiveProtobuf<WriteBytesRequest>()

                        val token = call.getAuthToken()
                        if (token != null && deviceCertificateState.checkPermission(
                                token,
                                request.path,
                                "write"
                            )
                        ) {
                            call.respond(HttpStatusCode.Forbidden, "没有权限写入该路径")
                            return@post
                        }

                        if (request.fileSize < 0L || request.blockIndex < 0L || request.blockLength < 0L || request.path.isEmpty()) {
                            call.respond(HttpStatusCode.BadRequest, "写入参数无效")
                            return@post
                        }

                        // 限制单次写入的数据大小，防止内存溢出
                        val maxBlockSize = MAX_LENGTH.toLong() * 4 // 最大32KB
                        if (request.blockLength > maxBlockSize) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                "单次写入数据过大，最大支持 ${maxBlockSize / 1024}KB"
                            )
                            return@post
                        }

                        // 检查实际数据大小是否与声明的大小匹配
                        if (request.byteArray.size.toLong() != request.blockLength) {
                            call.respond(HttpStatusCode.BadRequest, "数据大小与声明的块长度不匹配")
                            return@post
                        }

                        val writeResult = FileUtils.writeBytes(
                            request.path,
                            request.fileSize,
                            request.byteArray,
                            request.blockIndex * MAX_LENGTH
                        )
                        val success = writeResult.getOrDefault(false)
                        call.respondProtobuf(success)
                    } catch (e: OutOfMemoryError) {
                        call.respond(HttpStatusCode.InsufficientStorage, "内存不足，无法处理写入请求")
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "写入文件失败: ${e.message}")
                    }
                }

                post("/read-bytes") {
                    try {
                        val request = call.receiveProtobuf<ReadBytesRequest>()

                        val token = call.getAuthToken()
                        if (token != null && deviceCertificateState.checkPermission(
                                token,
                                request.path,
                                "read"
                            )
                        ) {
                            call.respond(HttpStatusCode.Forbidden, "没有权限读取该路径")
                            return@post
                        }

                        if (request.blockIndex < 0L || request.blockLength < 0L || request.path.isEmpty()) {
                            call.respond(HttpStatusCode.BadRequest, "读取参数无效")
                            return@post
                        }

                        // 限制单次读取的数据大小，防止内存溢出
                        val maxBlockSize = MAX_LENGTH.toLong() * 4 // 最大32KB
                        if (request.blockLength > maxBlockSize) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                "单次读取数据过大，最大支持 ${maxBlockSize / 1024}KB"
                            )
                            return@post
                        }

                        val file = java.io.File(request.path)
                        if (!file.exists()) {
                            call.respond(HttpStatusCode.NotFound, "文件不存在")
                            return@post
                        }

                        val fileSize = file.length()
                        val offset = request.blockIndex * MAX_LENGTH.toLong()
                        // 限制实际读取长度，防止内存溢出
                        val length = minOf(request.blockLength, maxBlockSize, fileSize - offset)

                        if (offset >= fileSize) {
                            call.respond(HttpStatusCode.BadRequest, "偏移量超出文件大小")
                            return@post
                        }

                        if (length <= 0) {
                            call.respond(HttpStatusCode.BadRequest, "无效的读取长度")
                            return@post
                        }

                        val readResult = FileUtils.readFileRange(
                            request.path,
                            offset,
                            offset + length - 1
                        )

                        if (readResult.isSuccess) {
                            val data = readResult.getOrNull() ?: byteArrayOf()
                            // 额外检查返回的数据大小
                            if (data.size > maxBlockSize.toInt()) {
                                call.respond(HttpStatusCode.InternalServerError, "读取的数据超出安全限制")
                                return@post
                            }
                            call.respondProtobuf(data)
                        } else {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "读取文件失败: ${readResult.exceptionOrNull()?.message}"
                            )
                        }
                    } catch (e: OutOfMemoryError) {
                        call.respond(HttpStatusCode.InsufficientStorage, "内存不足，无法处理读取请求")
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "读取文件失败: ${e.message}")
                    }
                }

                post("/read-file") {
                    try {
                        val request = call.receiveProtobuf<ReadFileChunksRequest>()

                        val token = call.getAuthToken()
                        if (token != null && deviceCertificateState.checkPermission(
                                token,
                                request.path,
                                "read"
                            )
                        ) {
                            call.respond(HttpStatusCode.Forbidden, "没有权限读取该路径")
                            return@post
                        }

                        // 获取文件信息但不读取内容
                        val file = java.io.File(request.path)
                        if (!file.exists()) {
                            call.respond(HttpStatusCode.NotFound, "文件不存在")
                            return@post
                        }

                        val fileSize = file.length()
                        val chunkSize = minOf(request.chunkSize, 1024 * 1024) // 限制最大1MB
                        val totalChunks = (fileSize + chunkSize - 1) / chunkSize

                        // 返回文件元数据而不是实际内容
                        val metadata = mapOf(
                            "fileSize" to fileSize,
                            "chunkSize" to chunkSize,
                            "totalChunks" to totalChunks,
                            "path" to request.path
                        )

                        call.respondProtobuf(metadata)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "读取文件失败: ${e.message}")
                    }
                }

                post("/get-file-by-path") {
                    try {
                        val request = call.receiveProtobuf<GetFileByPathRequest>()

                        val token = call.getAuthToken()
                        if (token != null && deviceCertificateState.checkPermission(
                                token,
                                request.path,
                                "read"
                            )
                        ) {
                            call.respond(HttpStatusCode.Forbidden, "没有权限访问该路径")
                            return@post
                        }

                        if (request.path.isEmpty()) {
                            call.respond(HttpStatusCode.BadRequest, "路径不能为空")
                            return@post
                        }

                        val result = FileUtils.getFile(request.path)
                        if (result.isSuccess) {
                            val fileInfo = result.getOrNull()?.apply {
                                protocol = FileProtocol.Device
                                protocolId = settings.getString("deviceId", "")
                            }
                            call.respondProtobuf(fileInfo)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "文件不存在")
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "获取文件信息失败: ${e.message}")
                    }
                }

                post("/get-file-by-path-and-name") {
                    try {
                        val request = call.receiveProtobuf<GetFileByPathAndNameRequest>()

                        val token = call.getAuthToken()
                        if (token != null && deviceCertificateState.checkPermission(
                                token,
                                "${request.path}${app.filemanager.utils.PathUtils.getPathSeparator()}${request.name}",
                                "read"
                            )
                        ) {
                            call.respond(HttpStatusCode.Forbidden, "没有权限访问该路径")
                            return@post
                        }

                        if (request.path.isEmpty() || request.name.isEmpty()) {
                            call.respond(HttpStatusCode.BadRequest, "路径和文件名不能为空")
                            return@post
                        }

                        val result = FileUtils.getFile(request.path, request.name)
                        if (result.isSuccess) {
                            val fileInfo = result.getOrNull()?.apply {
                                protocol = FileProtocol.Device
                                protocolId = settings.getString("deviceId", "")
                            }
                            call.respondProtobuf(fileInfo)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "文件不存在")
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "获取文件信息失败: ${e.message}")
                    }
                }

                post("/create-file") {
                    try {
                        val request = call.receiveProtobuf<CreateFileRequest>()

                        if (request.paths.isEmpty()) {
                            call.respond(HttpStatusCode.BadRequest, "文件路径不能为空")
                            return@post
                        }

                        val results = request.paths.map { path ->
                            val token = call.getAuthToken()
                            if (token != null && deviceCertificateState.checkPermission(
                                    token,
                                    path,
                                    "write"
                                )
                            ) {
                                return@map false
                            }

                            FileUtils.createFile(path).getOrDefault(false)
                        }
                        call.respondProtobuf(results)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "创建文件失败: ${e.message}")
                    }
                }
            }
        }
    }
}