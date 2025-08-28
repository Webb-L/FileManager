package app.filemanager.service.rpc.httproute

import app.filemanager.service.data.DeviceConnectRequest
import app.filemanager.service.data.DeviceConnectResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf

class DeviceRouteClient(
    private val baseUrl: String,
    private val token: String = "",
    private val httpClient: HttpClient
) {

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun connectDevice(request: DeviceConnectRequest): Result<DeviceConnectResponse> {
        return try {
            val response = httpClient.post("$baseUrl/api/devices/connect") {
                contentType(ContentType.Application.ProtoBuf)
                accept(ContentType.Application.ProtoBuf)
                setBody(request)
            }
            
            // 如果服务器没有设置正确的 Content-Type，手动解析
            val responseBody = if (response.contentType()?.match(ContentType.Application.ProtoBuf) == true) {
                response.body<DeviceConnectResponse>()
            } else {
                // 手动使用 ProtoBuf 解析响应字节
                val bytes = response.readRawBytes()
                ProtoBuf.decodeFromByteArray(DeviceConnectResponse.serializer(), bytes)
            }
            
            Result.success(responseBody)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}