package app.filemanager.service.rpc.httproute

import app.filemanager.data.main.DrawerBookmark
import app.filemanager.service.rpc.HttpRouteClientManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class BookmarkRouteClient(
    private val httpClient: HttpClient,
    private val manager: HttpRouteClientManager
) {

    suspend fun getBookmarks(): Result<List<DrawerBookmark>> {
        return try {
            val response = httpClient.get("/api/bookmarks")

            if (!response.status.isSuccess()) {
                throw Exception(response.bodyAsText())
            }

            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}