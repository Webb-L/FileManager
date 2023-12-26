package app.filemanager.utils

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import app.filemanager.data.FileInfo
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes

internal actual object PathUtils {
    // 获取用户目录
    actual fun getHomePath(): String = System.getProperty("user.home")

    // 获取路径分隔符
    actual fun getPathSeparator(): String = File.separator

    // 获取根目录
    actual fun getRootPaths(): List<String> = File.listRoots().map { it.path }

    // 遍历目录
    actual fun traverse(path: String): List<FileInfo> {
        val fileList = mutableListOf<FileInfo>()
        val directory = File(path)
        if (directory.exists()) {
            val files = directory.listFiles() ?: emptyArray()
            for (file in files) {
                try {
                    val absolutePath = file.absolutePath
                    val paths = Paths.get(absolutePath)
                    val attrs = Files.readAttributes(paths, BasicFileAttributes::class.java)
                    // TODO Windows 有问题。
                    var mineType = ""
                    if (file.isFile) {
                        mineType = file.extension.toLowerCase(Locale.current)
                    }
                    fileList.add(
                        FileInfo(
                            name = file.name,
                            description = "",
                            isDirectory = file.isDirectory,
                            isHidden = file.isHidden,
                            path = absolutePath,
                            mineType = mineType,
                            size = if (file.isDirectory) (file.listFiles()
                                ?: emptyArray<File>()).size.toLong() else file.length(),
                            permissions = 0,
                            user = "Files.getOwner(path).name",
                            userGroup = "attrs.group().name",
                            createdDate = attrs.creationTime().toMillis(),
                            updatedDate = attrs.lastModifiedTime().toMillis()
                        )
                    )
                    if (file.isDirectory) {
                        fileList.addAll(traverse(file.path))
                    }
                } catch (e: Exception) {

                }
            }
        }

        return fileList
    }
}