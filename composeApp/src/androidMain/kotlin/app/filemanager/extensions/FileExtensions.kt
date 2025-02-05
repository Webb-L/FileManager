package app.filemanager.extensions

import android.os.Build
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import app.filemanager.data.file.FileSimpleInfo
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes

fun File.toFileSimpleInfo(): Result<FileSimpleInfo> {
    val attrs = getFileAttributesCompat()
    var mineType = ""
    if (isFile) {
        val extension = extension.toLowerCase(Locale.current)
        if (extension.isNotEmpty()) {
            mineType = ".$extension"
        }
    }

    return Result.success(
        FileSimpleInfo(
            name = name,
            description = "",
            isDirectory = isDirectory,
            isHidden = isHidden,
            path = absolutePath,
            mineType = mineType,
            size = if (isDirectory) (listFiles() ?: emptyArray<File>()).size.toLong() else length(),
            createdDate = attrs["creationTime"] ?: 0L,
            updatedDate = attrs["lastModifiedTime"] ?: 0L
        )
    )
}

fun File.getFileAttributesCompat(): Map<String, Long> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // 在 API 26+ 使用 NIO
        val path = Paths.get(absolutePath)
        val attrs = Files.readAttributes(path, BasicFileAttributes::class.java)
        mapOf(
            "creationTime" to attrs.creationTime().toMillis(),
            "lastModifiedTime" to attrs.lastModifiedTime().toMillis()
        )
    } else {
        // 在更低版本使用 File 的方法作为替代
        val lastModified = lastModified()
        // 无法直接获取创建时间，若需精确保存创建时间，可自行维护
        mapOf(
            "creationTime" to 0L,
            "lastModifiedTime" to lastModified
        )
    }
}
