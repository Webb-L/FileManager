package app.filemanager.extensions

import app.filemanager.data.FileInfo
import app.filemanager.utils.PathUtils

fun String.replaceLast(oldValue: String, newValue: String): String {
    val lastIndex = this.lastIndexOf(oldValue)
    if (lastIndex < 0) return this
    return this.substring(0, lastIndex) + newValue + this.substring(lastIndex + oldValue.length)
}

fun String.getFileAndFolder(): List<FileInfo> = PathUtils.getFileAndFolder(this)
internal expect fun String.parsePath(): List<String>