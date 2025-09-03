package app.filemanager.service.rpc.httproute

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.FileSizeInfo
import app.filemanager.service.data.*
import app.filemanager.service.rpc.HttpRouteClientManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class FileRouteClient(
    private val httpClient: HttpClient,
    private val manager: HttpRouteClientManager
) {
    suspend fun renames(request: RenameRequest): Result<List<Result<Boolean>>> {
        return try {
            val response = httpClient.post("/api/files/rename") {
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body<List<SerializableResult<Boolean>>>().map { result -> result.toResult() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFolders(request: CreateFolderRequest): Result<List<Result<Boolean>>> {
        return try {
            val response = httpClient.post("/api/files/create-folder") {
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body<List<SerializableResult<Boolean>>>().map { result -> result.toResult() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSizeInfo(request: GetSizeInfoRequest): Result<FileSizeInfo> {
        return try {
            val response = httpClient.post("/api/files/size-info") {
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletes(request: DeleteRequest): Result<List<Result<Boolean>>> {
        return try {
            val response = httpClient.post("/api/files/delete") {
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body<List<SerializableResult<Boolean>>>().map { result -> result.toResult() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeBytes(request: WriteBytesRequest): Result<Boolean> {
        return try {
            val response = httpClient.post("/api/files/write-bytes") {
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readBytes(request: ReadBytesRequest): Result<ByteArray> {
        return try {
            val response = httpClient.post("/api/files/read-bytes") {
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // TODO api 待定
    suspend fun readFile(request: ReadFileChunksRequest): Result<Map<String, Any>> {
        return try {
            val response = httpClient.post("/api/files/read-file") {
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFileByPath(request: GetFileByPathRequest): Result<FileSimpleInfo> {
        return try {
            val response = httpClient.post("/api/files/get-file-by-path") {
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFileByPathAndName(request: GetFileByPathAndNameRequest): Result<FileSimpleInfo> {
        return try {
            val response = httpClient.post("/api/files/get-file-by-path-and-name") {
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFiles(request: CreateFileRequest): Result<List<Result<Boolean>>> {
        return try {
            val response = httpClient.post("/api/files/create-file") {
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body<List<SerializableResult<Boolean>>>().map { result -> result.toResult() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}