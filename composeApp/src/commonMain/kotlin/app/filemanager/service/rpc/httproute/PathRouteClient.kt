package app.filemanager.service.rpc.httproute

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.PathInfo
import app.filemanager.service.data.ListRequest
import app.filemanager.service.data.SerializableResult
import app.filemanager.service.data.TraversePathRequest
import app.filemanager.service.data.toResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class PathRouteClient(
    private val baseUrl: String,
    private val token: String = "",
    private val httpClient: HttpClient
) {
    suspend fun getRootPaths(): Result<List<PathInfo>> {
        return try {
            val response = httpClient.post("$baseUrl/api/paths/rootPaths") {
                contentType(ContentType.Application.ProtoBuf)
                if (token.isNotEmpty()) {
                    header("Authorization", "Bearer $token")
                }
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

            val response = httpClient.post("$baseUrl/api/paths/list") {
                contentType(ContentType.Application.ProtoBuf)
                if (token.isNotEmpty()) {
                    header("Authorization", "Bearer $token")
                }
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
                val response = httpClient.post("$baseUrl/api/paths/traverse") {
                    contentType(ContentType.Application.ProtoBuf)
                    if (token.isNotEmpty()) {
                        header("Authorization", "Bearer $token")
                    }
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

}