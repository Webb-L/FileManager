package app.filemanager.service.rpc.httproute

import app.filemanager.service.data.DeviceConnectRequest
import app.filemanager.service.data.DeviceConnectResponse
import app.filemanager.service.rpc.HttpRouteClientManager
import app.filemanager.utils.CryptoProtoBuf
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf

class DeviceRouteClient(
    private val httpClient: HttpClient,
    private val manager: HttpRouteClientManager
) {

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun connectDevice(request: DeviceConnectRequest): Result<DeviceConnectResponse> {
        return try {
            val response = httpClient.post("/api/devices/connect") {
                setBody(CryptoProtoBuf.encode(request))
            }

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            // 如果服务器没有设置正确的 Content-Type，手动解析
            val responseBody: DeviceConnectResponse = CryptoProtoBuf.decode(response.body())

            Result.success(responseBody)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
