package app.filemanager.extensions

import app.filemanager.data.FileInfo
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFileAttributes


internal actual fun String.getAllFilesInDirectory(): List<FileInfo> =
    (File(this).listFiles() ?: emptyArray<File>()).map { file ->
        val path = file.absolutePath
        val attrs = Files.readAttributes(
            Paths.get(path),
            PosixFileAttributes::class.java
        )
        FileInfo(
            name = file.name,
            description = "",
            isDirectory = file.isDirectory,
            isHidden = file.isHidden,
            path = path,
            mineType = file.extension,
            size = if (file.isDirectory) (file.listFiles() ?: emptyArray<File>()).size.toLong() else file.length(),
            permissions = 0,
            user = attrs.owner().name,
            userGroup = attrs.group().name,
            createdDate = attrs.creationTime().toMillis(),
            updatedDate = attrs.lastModifiedTime().toMillis()
        )
    }
