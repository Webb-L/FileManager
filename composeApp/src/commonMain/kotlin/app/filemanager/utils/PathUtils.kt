package app.filemanager.utils

internal expect object PathUtils {
    fun getHomePath(): String
    fun getPathSeparator(): String
    fun getRootPaths(): List<String>
}
