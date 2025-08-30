package app.filemanager.service.data

import app.filemanager.exception.EmptyDataException
import app.filemanager.exception.AuthorityException
import app.filemanager.exception.ParameterErrorException
import app.filemanager.exception.TimeoutException
import kotlinx.serialization.Serializable

@Serializable
data class SerializableResult<T>(
    val isSuccess: Boolean,
    val data: T? = null,
    val errorMessage: String? = null,
    val errorType: String? = null
) {
    companion object {
        fun <T> success(data: T): SerializableResult<T> {
            return SerializableResult(isSuccess = true, data = data)
        }

        fun <T> failure(errorMessage: String, errorType: String? = null): SerializableResult<T> {
            return SerializableResult(isSuccess = false, errorMessage = errorMessage, errorType = errorType)
        }

        fun <T> failure(exception: Throwable): SerializableResult<T> {
            return SerializableResult(
                isSuccess = false, 
                errorMessage = exception.message ?: "Unknown error",
                errorType = exception::class.simpleName
            )
        }
    }
}

fun <T> Result<T>.toSerializableResult(): SerializableResult<T> {
    return if (isSuccess) {
        SerializableResult.success(getOrThrow())
    } else {
        val exception = exceptionOrNull()
        SerializableResult.failure(
            errorMessage = exception?.message ?: "Unknown error",
            errorType = exception?.let { it::class.simpleName }
        )
    }
}

fun <T> SerializableResult<T>.toResult(): Result<T> {
    return if (isSuccess && data != null) {
        Result.success(data)
    } else {
        val exception = recreateException(errorType, errorMessage ?: "Unknown error")
        Result.failure(exception)
    }
}

private fun recreateException(errorType: String?, errorMessage: String): Exception {
    return when (errorType) {
        "EmptyDataException" -> EmptyDataException(errorMessage)
        "AuthorityException" -> AuthorityException(errorMessage)
        "ParameterErrorException" -> ParameterErrorException(errorMessage)
        "TimeoutException" -> TimeoutException(errorMessage)
        else -> Exception(errorMessage)
    }
}