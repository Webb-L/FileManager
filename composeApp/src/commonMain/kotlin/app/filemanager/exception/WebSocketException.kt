package app.filemanager.exception

import app.filemanager.service.WebSocketResult
import kotlinx.serialization.Serializable

@Serializable
class ParameterErrorException(override val message: String = "参数错误") : Exception(message)

// 权限相关的错误
@Serializable
data class AuthorityException(override val message: String?) : Exception(message)

// 权限相关的错误
@Serializable
class EmptyDataException : Exception()

fun Exception.toSocketResult(): WebSocketResult<Nothing> = WebSocketResult(
    message,
    this::class.simpleName,
    null
)