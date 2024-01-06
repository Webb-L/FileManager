package app.filemanager.utils

import app.filemanager.data.file.FileInfo

internal expect object PathUtils {
    fun getFileAndFolder(path: String): List<FileInfo>
    fun getAppPath(): String
    fun getHomePath(): String
    fun getPathSeparator(): String
    fun getRootPaths(): List<String>
    fun traverse(path: String): List<FileInfo>
}
