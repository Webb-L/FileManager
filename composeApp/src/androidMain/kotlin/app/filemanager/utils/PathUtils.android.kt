package app.filemanager.utils

import android.os.Environment
import app.filemanager.data.file.FileInfo
import app.filemanager.data.main.DrawerBookmark
import app.filemanager.data.main.DrawerBookmarkType
import app.filemanager.extensions.toFileInfo
import java.io.File

internal actual object PathUtils {
    // 获取目录下所有文件和文件夹
    actual fun getFileAndFolder(path: String): List<FileInfo> =
        (File(path).listFiles() ?: emptyArray<File>()).map { file ->
            file.toFileInfo()
        }

    // 获取用户目录
    actual fun getAppPath(): String = System.getProperty("user.dir")

    // 获取用户目录
    actual fun getHomePath(): String = Environment.getExternalStorageDirectory().absolutePath;

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

    actual fun getBookmarks(): List<DrawerBookmark> {
        val homePath = getHomePath()

        return listOf(
            DrawerBookmark(name = "主目录", path = homePath, iconType = DrawerBookmarkType.Home),
            DrawerBookmark(
                name = "图片",
                path = "$homePath${File.separator}Pictures",
                iconType = DrawerBookmarkType.Image
            ),
            DrawerBookmark(
                name = "音乐",
                path = "$homePath${File.separator}Music",
                iconType = DrawerBookmarkType.Audio
            ),
            DrawerBookmark(
                name = "视频",
                path = "$homePath${File.separator}Movies",
                iconType = DrawerBookmarkType.Video
            ),
            DrawerBookmark(
                name = "文档",
                path = "$homePath${File.separator}Documents",
                iconType = DrawerBookmarkType.Document
            ),
            DrawerBookmark(
                name = "下载",
                path = "$homePath${File.separator}Download",
                iconType = DrawerBookmarkType.Download
            ),
        )
    }
}