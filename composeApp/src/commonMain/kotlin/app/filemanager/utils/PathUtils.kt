package app.filemanager.utils

import app.filemanager.data.file.FileInfo
import app.filemanager.data.main.DrawerBookmark

internal expect object PathUtils {
    fun getFileAndFolder(path: String): List<FileInfo>
    fun getAppPath(): String
    fun getHomePath(): String
    fun getPathSeparator(): String
    fun getRootPaths(): List<String>
    fun traverse(path: String): List<FileInfo>
    fun getBookmarks(): List<DrawerBookmark>
}
