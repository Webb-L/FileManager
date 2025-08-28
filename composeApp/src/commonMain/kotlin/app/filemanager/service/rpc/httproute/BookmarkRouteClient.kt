package app.filemanager.service.rpc.httproute

import app.filemanager.data.main.DrawerBookmark
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class BookmarkRouteClient(
    private val baseUrl: String,
    private val token: String = "",
    private val httpClient: HttpClient
) {

    suspend fun getBookmarks(): Result<List<DrawerBookmark>> {
        return try {
            val response = httpClient.get("$baseUrl/api/bookmarks") {
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

}