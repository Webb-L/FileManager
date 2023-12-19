package app.filemanager.utils

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

}