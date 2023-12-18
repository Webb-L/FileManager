package app.filemanager.utils

internal expect fun getHomePath(): String

internal expect fun getPathSeparator():String
internal expect fun getRootPaths(): List<String>