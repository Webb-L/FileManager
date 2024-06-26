package app.filemanager.exception

import kotlinx.serialization.Serializable

// 权限相关的错误
@Serializable
data class AuthorityException(override val message: String?) : Exception(message)