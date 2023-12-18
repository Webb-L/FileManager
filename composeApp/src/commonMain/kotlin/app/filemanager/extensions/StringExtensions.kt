package app.filemanager.extensions

import app.filemanager.data.FileInfo

internal expect fun String.getAllFilesInDirectory(): List<FileInfo>
internal expect fun String.parsePath(): List<String>