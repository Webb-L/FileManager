package app.filemanager.service.routes

import app.filemanager.createSettings
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.service.data.toSerializableResult
import app.filemanager.service.plugins.ProtobufRequest
import app.filemanager.service.plugins.receiveProtobuf
import app.filemanager.service.plugins.respondProtobuf
import app.filemanager.utils.PathUtils
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import app.filemanager.utils.SymmetricCrypto
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class TraversePathRequest(
    val path: String
)

@Serializable
data class ListRequest(
    val path: String
)

@OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
fun Route.pathRoutes() {
    val settings = createSettings()

    route("/api/paths") {
        install(ProtobufRequest)

        post("/traverse") {
            try {
                val request = call.receiveProtobuf<TraversePathRequest>()

                // 设置SSE响应头
                call.response.headers.append(HttpHeaders.ContentType, "text/event-stream")
                call.response.headers.append(HttpHeaders.CacheControl, "no-cache")
                call.response.headers.append(HttpHeaders.Connection, "keep-alive")
                call.response.headers.append("Access-Control-Allow-Origin", "*")

                // 直接实现traversePath逻辑，返回文件映射
                var isFirst = false
                val traverseFlow = channelFlow {
                    PathUtils.traverse(request.path) { fileAndFolder ->
                        val result = if (fileAndFolder.isFailure) {
                            emptyMap()
                        } else {
                            mutableMapOf<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>().apply {
                                val files: MutableList<FileSimpleInfo> = mutableListOf()
                                files.addAll(fileAndFolder.getOrDefault(listOf()))
                                if (!isFirst) {
                                    val fileResult = app.filemanager.utils.FileUtils.getFile(request.path)
                                    if (fileResult.isSuccess) {
                                        fileResult.getOrNull()?.let { files.add(it) }
                                    }
                                    isFirst = true
                                }
                                files.forEach { fileSimpleInfo ->
                                    val key = if (fileSimpleInfo.protocol == FileProtocol.Local)
                                        Pair(FileProtocol.Device, settings.getString("deviceId", ""))
                                    else
                                        Pair(fileSimpleInfo.protocol, fileSimpleInfo.protocolId)

                                    if (!containsKey(key)) {
                                        put(key, mutableListOf(fileSimpleInfo.apply {
                                            this.path = this.path.replace(request.path, "")
                                            this.protocol = FileProtocol.Local
                                            this.protocolId = ""
                                        }))
                                    } else {
                                        get(key)?.add(fileSimpleInfo.apply {
                                            this.path = this.path.replace(request.path, "")
                                            this.protocol = FileProtocol.Local
                                            this.protocolId = ""
                                        })
                                    }
                                }
                            }
                        }

                        launch(Dispatchers.Default) {
                            send(result)
                        }
                    }
                }

                // 使用respondTextWriter手动实现SSE响应
                call.respondTextWriter(ContentType.Text.EventStream) {
                    traverseFlow.collect { result ->
                        val responseBytes = ProtoBuf.encodeToByteArray(result)
                        // 加密并使用Base64编码确保数据传输完整性
                        val data = Base64.encode(SymmetricCrypto.encrypt(responseBytes))

                        write("event: traverseResult\n")
                        write("data: $data\n")
                        write("\n")
                        flush()
                    }
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "遍历路径失败: ${e.message}")
            }
        }

        // POST端点需要ProtobufRequest插件
        post("/list") {
            try {
                val request = call.receiveProtobuf<ListRequest>()

                // 直接实现list逻辑，返回文件映射
                if (request.path.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, "路径不能为空")
                    return@post
                }

                val fileAndFolder = request.path.getFileAndFolder()
                if (fileAndFolder.isFailure) {
                    call.respondProtobuf(fileAndFolder.toSerializableResult())
                    return@post
                }

                call.respondProtobuf(Result.success(mutableMapOf<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>().apply {
                    fileAndFolder.getOrNull()?.forEach { fileSimpleInfo ->
                        val key = if (fileSimpleInfo.protocol == FileProtocol.Local)
                            Pair(FileProtocol.Device, settings.getString("deviceId", ""))
                        else
                            Pair(fileSimpleInfo.protocol, fileSimpleInfo.protocolId)

                        if (!containsKey(key)) {
                            put(key, mutableListOf(fileSimpleInfo.apply {
                                this.path = this.path.replace(request.path, "")
                                this.protocol = FileProtocol.Local
                                this.protocolId = ""
                            }))
                        } else {
                            get(key)?.add(fileSimpleInfo.apply {
                                this.path = this.path.replace(request.path, "")
                                this.protocol = FileProtocol.Local
                                this.protocolId = ""
                            })
                        }
                    }
                }).toSerializableResult())
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "获取文件列表失败: ${e.message}")
            }
        }

        post("/rootPaths") {
            try {
                // 直接实现rootPaths逻辑，返回路径列表
                val rootPaths = PathUtils.getRootPaths()
                val result = if (rootPaths.isFailure) {
                    emptyList()
                } else {
                    rootPaths.getOrNull() ?: emptyList()
                }

                call.respondProtobuf(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "获取根路径失败: ${e.message}")
            }
        }
    }
}
