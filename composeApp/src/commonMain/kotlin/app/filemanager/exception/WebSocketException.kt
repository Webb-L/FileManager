package app.filemanager.exception

import kotlinx.serialization.Serializable

@Serializable
class ParameterErrorException(override val message: String = "参数错误") : Exception(message)