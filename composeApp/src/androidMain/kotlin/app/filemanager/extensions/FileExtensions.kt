package app.filemanager.extensions

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import app.filemanager.data.file.FileInfo
import app.filemanager.data.file.FileSimpleInfo
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes

fun File.toFileSimpleInfo(): FileSimpleInfo {
    val path = Paths.get(absolutePath)
    val attrs = Files.readAttributes(path, BasicFileAttributes::class.java)
    var mineType = ""
    if (isFile) {
        val extension = extension.toLowerCase(Locale.current)
        if (extension.isNotEmpty()) {
            mineType = ".$extension"
        }
    }

    return FileSimpleInfo(
        name = name,
        description = "",
        isDirectory = isDirectory,
        isHidden = isHidden,
        path = absolutePath,
        mineType = mineType,
        size = if (isDirectory) (listFiles() ?: emptyArray<File>()).size.toLong() else length(),
        createdDate = attrs.creationTime().toMillis(),
        updatedDate = attrs.lastModifiedTime().toMillis()
    )
}

fun File.toFileInfo(): FileInfo {
    // TODO Windows 有问题。
    // println(Files.getPosixFilePermissions(path))
    // 输出例如 [OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ]
    return toFileSimpleInfo().toFileInfo(
        permissions = 0,
        user = "Files.getOwner(path).name",
        userGroup = "attrs.group().name",
    )
}