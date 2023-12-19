package app.filemanager.extensions

import app.filemanager.data.FileInfo

internal actual fun String.getAllFilesInDirectory(): List<FileInfo> {
    return listOf()
}

internal actual fun String.parsePath(): List<String> {
    return listOf("sdfasfsfasfasfs","sdfasfsdfs","dsfasfsdfaafs","sdfsafsfsfdadf","29034809810942")
}