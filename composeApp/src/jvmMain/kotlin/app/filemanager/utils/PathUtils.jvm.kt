package app.filemanager.utils

import app.filemanager.data.file.FileInfo
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.main.DrawerBookmark
import app.filemanager.data.main.DrawerBookmarkType
import app.filemanager.extensions.toFileInfo
import app.filemanager.extensions.toFileSimpleInfo
import java.io.File
import java.io.File.separator

internal actual object PathUtils {
    // 获取目录下所有文件和文件夹
    actual fun getFileAndFolder(path: String): List<FileSimpleInfo> =
        (File(path).listFiles() ?: emptyArray<File>()).map { file ->
            file.toFileSimpleInfo()
        }

    // 获取用户目录
    actual fun getAppPath(): String = System.getProperty("user.dir")

    // 获取用户目录
    actual fun getHomePath(): String = System.getProperty("user.home")

    // 获取路径分隔符
    actual fun getPathSeparator(): String = File.separator

    // 获取根目录
    actual fun getRootPaths(): List<String> = File.listRoots().map { it.path }

    // 遍历目录
    actual fun traverse(path: String): List<FileSimpleInfo> {
        val fileList = mutableListOf<FileSimpleInfo>()
        val directory = File(path)
        if (directory.exists()) {
            if (directory.isDirectory) {
                val files = directory.listFiles() ?: emptyArray()
                for (file in files) {
                    try {
                        fileList.add(file.toFileSimpleInfo())
                        if (file.isDirectory) {
                            fileList.addAll(traverse(file.path))
                        }
                    } catch (e: Exception) {
                    }
                }
                if (files.isEmpty()) {
                    fileList.add(directory.toFileSimpleInfo())
                }
            } else {
                fileList.add(directory.toFileSimpleInfo())
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
                path = "$homePath${separator}Pictures",
                iconType = DrawerBookmarkType.Image
            ),
            DrawerBookmark(name = "音乐", path = "$homePath${separator}Music", iconType = DrawerBookmarkType.Audio),
            DrawerBookmark(
                name = "视频",
                path = "$homePath${separator}Videos",
                iconType = DrawerBookmarkType.Video
            ),
            DrawerBookmark(
                name = "文档",
                path = "$homePath${separator}Documents",
                iconType = DrawerBookmarkType.Document
            ),
            DrawerBookmark(
                name = "下载",
                path = "$homePath${separator}Downloads",
                iconType = DrawerBookmarkType.Download
            ),
        )
    }
}