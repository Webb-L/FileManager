package app.filemanager.utils

import app.filemanager.data.file.FileInfo

internal actual object PathUtils {
    actual fun getHomePath(): String {
        return "/"
    }

    actual fun getPathSeparator(): String {
        return "/"
    }

    actual fun getRootPaths(): List<String> {
        return listOf("/")
    }

    actual fun getFileAndFolder(path: String): List<FileInfo> {
        return emptyList()
    }

    actual fun traverse(path: String): List<FileInfo> {
        return emptyList()
    }

}