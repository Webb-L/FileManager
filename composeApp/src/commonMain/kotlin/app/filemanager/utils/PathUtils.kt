package app.filemanager.utils

import app.filemanager.data.FileInfo

internal expect object PathUtils {
    fun getHomePath(): String
    fun getPathSeparator(): String
    fun getRootPaths(): List<String>
    fun traverse(path: String): List<FileInfo>
}
