package app.filemanager.extensions

import app.filemanager.data.FileInfo
import app.filemanager.utils.PathUtils

fun String.getFileAndFolder(): List<FileInfo> = PathUtils.getFileAndFolder(this)
internal expect fun String.parsePath(): List<String>