package app.filemanager.service.routes

import app.filemanager.extensions.respondProtobuf
import app.filemanager.service.plugins.PermissionCheck
import app.filemanager.utils.PathUtils
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.bookmarkRoutes() {
    route("/api/bookmarks") {
        install(PermissionCheck) {
            resource = "bookmark"
            action = "read"
        }
        
        get {
            try {
                val bookmarks = PathUtils.getBookmarks()
                call.respondProtobuf(bookmarks)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "获取书签失败: ${e.message}")
            }
        }
    }
}