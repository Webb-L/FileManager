package app.filemanager.utils

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.main.DrawerBookmark

internal expect object PathUtils {
    fun getFileAndFolder(path: String): Result<List<FileSimpleInfo>>
    fun getAppPath(): String
    fun getHomePath(): String
    fun getPathSeparator(): String
    fun getRootPaths(): List<String>
    fun traverse(path: String): List<FileSimpleInfo>
    fun getBookmarks(): List<DrawerBookmark>
}
