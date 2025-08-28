package app.filemanager.service.rpc.httproute

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.FileSizeInfo
import app.filemanager.service.data.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class FileRouteClient(
    private val baseUrl: String,
    private val token: String = "",
    private val httpClient: HttpClient
) {

    suspend fun renames(request: RenameRequest): Result<List<Result<Boolean>>> {
        return try {
            val response = httpClient.post("$baseUrl/api/files/rename") {
                contentType(ContentType.Application.ProtoBuf)
                if (token.isNotEmpty()) {
                    header("Authorization", "Bearer $token")
                }
                setBody(request)
            }
            Result.success(response.body<List<SerializableResult<Boolean>>>().map { result -> result.toResult() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFolders(request: CreateFolderRequest): Result<List<Result<Boolean>>> {
        return try {
            val response = httpClient.post("$baseUrl/api/files/create-folder") {
                contentType(ContentType.Application.ProtoBuf)
                if (token.isNotEmpty()) {
                    header("Authorization", "Bearer $token")
                }
                setBody(request)
            }
            Result.success(response.body<List<SerializableResult<Boolean>>>().map { result -> result.toResult() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSizeInfo(request: GetSizeInfoRequest): Result<FileSizeInfo> {
        return try {
            val response = httpClient.post("$baseUrl/api/files/size-info") {
                contentType(ContentType.Application.ProtoBuf)
                if (token.isNotEmpty()) {
                    header("Authorization", "Bearer $token")
                }
                setBody(request)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletes(request: DeleteRequest): Result<List<Result<Boolean>>> {
        return try {
            val response = httpClient.post("$baseUrl/api/files/delete") {
                contentType(ContentType.Application.ProtoBuf)
                if (token.isNotEmpty()) {
                    header("Authorization", "Bearer $token")
                }
                setBody(request)
            }
            Result.success(response.body<List<SerializableResult<Boolean>>>().map { result -> result.toResult() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeBytes(request: WriteBytesRequest): Result<Boolean> {
        return try {
            val response = httpClient.post("$baseUrl/api/files/write-bytes") {
                contentType(ContentType.Application.ProtoBuf)
                if (token.isNotEmpty()) {
                    header("Authorization", "Bearer $token")
                }
                setBody(request)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readBytes(request: ReadBytesRequest): Result<ByteArray> {
        return try {
            val response = httpClient.post("$baseUrl/api/files/read-bytes") {
                contentType(ContentType.Application.ProtoBuf)
                if (token.isNotEmpty()) {
                    header("Authorization", "Bearer $token")
                }
                setBody(request)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // TODO api 待定
    suspend fun readFile(request: ReadFileChunksRequest): Result<Map<String, Any>> {
        return try {
            val response = httpClient.post("$baseUrl/api/files/read-file") {
                contentType(ContentType.Application.ProtoBuf)
                if (token.isNotEmpty()) {
                    header("Authorization", "Bearer $token")
                }
                setBody(request)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFileByPath(request: GetFileByPathRequest): Result<FileSimpleInfo> {
        return try {
            val response = httpClient.post("$baseUrl/api/files/get-file-by-path") {
                contentType(ContentType.Application.ProtoBuf)
                if (token.isNotEmpty()) {
                    header("Authorization", "Bearer $token")
                }
                setBody(request)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFileByPathAndName(request: GetFileByPathAndNameRequest): Result<FileSimpleInfo> {
        return try {
            val response = httpClient.post("$baseUrl/api/files/get-file-by-path-and-name") {
                contentType(ContentType.Application.ProtoBuf)
                if (token.isNotEmpty()) {
                    header("Authorization", "Bearer $token")
                }
                setBody(request)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFiles(request: CreateFileRequest): Result<List<Result<Boolean>>> {
        return try {
            val response = httpClient.post("$baseUrl/api/files/create-file") {
                contentType(ContentType.Application.ProtoBuf)
                if (token.isNotEmpty()) {
                    header("Authorization", "Bearer $token")
                }
                setBody(request)
            }
            Result.success(response.body<List<SerializableResult<Boolean>>>().map { result -> result.toResult() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}