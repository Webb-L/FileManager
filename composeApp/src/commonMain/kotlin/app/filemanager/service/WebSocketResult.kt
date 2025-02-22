package app.filemanager.service

import app.filemanager.exception.AuthorityException
import app.filemanager.exception.EmptyDataException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
class WebSocketResult<out T>(
    val message: String? = null,
    val className: String? = null,
    @Contextual val value: T?,
) {
    val isSuccess: Boolean get() = message == null && className == null

    fun deSerializable(): Exception {
        println("[deSerializable] className = $className, message = $message")
        return when (className) {
            "EmptyDataException" -> EmptyDataException()
            "AuthorityException" -> AuthorityException(message)
            else -> Exception(message)
        }
    }
}