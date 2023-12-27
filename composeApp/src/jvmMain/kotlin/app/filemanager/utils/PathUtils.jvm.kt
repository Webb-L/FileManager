package app.filemanager.utils

import app.filemanager.data.FileInfo
import app.filemanager.extensions.toFileInfo
import java.io.File

internal actual object PathUtils {
    // 获取目录下所有文件和文件夹
    actual fun getFileAndFolder(path: String): List<FileInfo> =
        (File(path).listFiles() ?: emptyArray<File>()).map { file ->
            file.toFileInfo()
        }

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
            if (directory.isDirectory) {
                val files = directory.listFiles() ?: emptyArray()
                for (file in files) {
                    try {
                        fileList.add(file.toFileInfo())
                        if (file.isDirectory) {
                            fileList.addAll(traverse(file.path))
                        }
                    } catch (e: Exception) {
                    }
                }
                if (files.isEmpty()) {
                    fileList.add(directory.toFileInfo())
                }
            } else {
                fileList.add(directory.toFileInfo())
            }
        }

        return fileList
    }
}