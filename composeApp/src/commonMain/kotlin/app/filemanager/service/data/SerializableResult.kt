package app.filemanager.service.data

import kotlinx.serialization.Serializable

@Serializable
data class SerializableResult<T>(
    val isSuccess: Boolean,
    val data: T? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun <T> success(data: T): SerializableResult<T> {
            return SerializableResult(isSuccess = true, data = data)
        }

        fun <T> failure(errorMessage: String): SerializableResult<T> {
            return SerializableResult(isSuccess = false, errorMessage = errorMessage)
        }

        fun <T> failure(exception: Throwable): SerializableResult<T> {
            return SerializableResult(isSuccess = false, errorMessage = exception.message ?: "Unknown error")
        }
    }
}

fun <T> Result<T>.toSerializableResult(): SerializableResult<T> {
    return if (isSuccess) {
        SerializableResult.success(getOrThrow())
    } else {
        SerializableResult.failure(exceptionOrNull()?.message ?: "Unknown error")
    }
}

fun <T> SerializableResult<T>.toResult(): Result<T> {
    return if (isSuccess && data != null) {
        Result.success(data)
    } else {
        Result.failure(Exception(errorMessage ?: "Unknown error"))
    }
}