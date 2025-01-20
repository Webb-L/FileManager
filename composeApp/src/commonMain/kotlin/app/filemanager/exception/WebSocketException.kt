package app.filemanager.exception

import app.filemanager.service.WebSocketResult
import kotlinx.serialization.Serializable

@Serializable
class TimeoutException(override val message: String = "超时错误") : Exception()

@Serializable
class ParameterErrorException(override val message: String = "参数错误") : Exception(message)

@Serializable
data class AuthorityException(override val message: String?) : Exception(message)

@Serializable
class EmptyDataException : Exception()

fun Exception.toSocketResult(): WebSocketResult<Nothing> = WebSocketResult(
    message,
    this::class.simpleName,
    null
)