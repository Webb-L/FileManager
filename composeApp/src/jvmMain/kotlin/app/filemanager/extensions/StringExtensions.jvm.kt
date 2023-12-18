package app.filemanager.extensions

import app.filemanager.data.FileInfo
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.name

internal actual fun String.getAllFilesInDirectory(): List<FileInfo> =
    (File(this).listFiles() ?: emptyArray<File>()).map { file ->
        val absolutePath = file.absolutePath
        val path = Paths.get(absolutePath)
        val attrs = Files.readAttributes(path, BasicFileAttributes::class.java)
        // TODO Windows 有问题。
        FileInfo(
            name = file.name,
            description = "",
            isDirectory = file.isDirectory,
            isHidden = file.isHidden,
            path = absolutePath,
            mineType = file.extension,
            size = if (file.isDirectory) (file.listFiles() ?: emptyArray<File>()).size.toLong() else file.length(),
            permissions = 0,
            user = "Files.getOwner(path).name",
            userGroup = "attrs.group().name",
            createdDate = attrs.creationTime().toMillis(),
            updatedDate = attrs.lastModifiedTime().toMillis()
        )
    }

internal actual fun String.parsePath(): List<String> = Paths.get(this).map { it.name }