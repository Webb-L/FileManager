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
    suspend fun renames(renameInfos: List<RenameInfo>): Result<List<Result<Boolean>>> {
        return try {
            val response = httpClient.post("/api/files/rename") {
                setBody(RenameRequest(renameInfos))
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body<List<SerializableResult<Boolean>>>().map { result -> result.toResult() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFolders(infos: List<CreateInfo>): Result<List<Result<Boolean>>> {
        return try {
            val response = httpClient.post("/api/files/create-folder") {
                setBody(CreateFolderRequest(infos))
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body<List<SerializableResult<Boolean>>>().map { result -> result.toResult() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSizeInfo(totalSpace: Long, freeSpace: Long, fileSimpleInfo: FileSimpleInfo): Result<FileSizeInfo> {
        return try {
            val response = httpClient.post("/api/files/size-info") {
                setBody(GetSizeInfoRequest(totalSpace, freeSpace, fileSimpleInfo))
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletes(paths: List<String>): Result<List<Result<Boolean>>> {
        return try {
            val response = httpClient.post("/api/files/delete") {
                setBody(DeleteRequest(paths))
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body<List<SerializableResult<Boolean>>>().map { result -> result.toResult() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeBytes(
        fileSize: Long,
        blockIndex: Long,
        blockLength: Long,
        path: String,
        byteArray: ByteArray
    ): Result<Boolean> {
        return try {
            val response = httpClient.post("/api/files/write-bytes") {
                setBody(
                    WriteBytesRequest(
                        fileSize = fileSize,
                        blockIndex = blockIndex,
                        blockLength = blockLength,
                        path = path,
                        byteArray = byteArray
                    )
                )
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readBytes(path: String, startOffset: Long, endOffset: Long): Result<ByteArray> {
        return try {
            val response = httpClient.post("/api/files/read-bytes") {
                setBody(ReadBytesRequest(path, startOffset, endOffset))
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
    suspend fun readFile(path: String, chunkSize: Long): Result<Map<String, Any>> {
        return try {
            val response = httpClient.post("/api/files/read-file") {
                setBody(ReadFileChunksRequest(path, chunkSize))
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFileByPath(path: String): Result<FileSimpleInfo> {
        return try {
            val response = httpClient.post("/api/files/get-file-by-path") {
                setBody(GetFileByPathRequest(path))
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFileByPathAndName(path: String, name: String): Result<FileSimpleInfo> {
        return try {
            val response = httpClient.post("/api/files/get-file-by-path-and-name") {
                setBody(GetFileByPathAndNameRequest(path, name))
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFiles(infos: List<CreateInfo>): Result<List<Result<Boolean>>> {
        return try {
            val response = httpClient.post("/api/files/create-file") {
                setBody(CreateFileRequest(infos))
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
