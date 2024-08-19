package app.filemanager.utils

import android.os.Environment
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.PathInfo
import app.filemanager.data.main.DrawerBookmark
import app.filemanager.data.main.DrawerBookmarkType
import app.filemanager.exception.AuthorityException
import app.filemanager.exception.EmptyDataException
import app.filemanager.extensions.toFileSimpleInfo
import java.io.File

internal actual object PathUtils {
    // 获取目录下所有文件和文件夹
    actual fun getFileAndFolder(path: String): Result<List<FileSimpleInfo>> {
        val file = File(path)
        if (!file.exists()) return Result.failure(AuthorityException("该目录并不存在"))
        if (!file.canRead()) return Result.failure(AuthorityException("没有权限"))

        val listFiles = file.listFiles() ?: return Result.failure(EmptyDataException())
        if (listFiles.isEmpty()) return Result.failure(EmptyDataException())

        return Result.success(listFiles.map { file ->
            file.toFileSimpleInfo()
        })
    }

    // 获取用户目录
    actual fun getAppPath(): String = System.getProperty("user.dir")

    // 获取用户目录
    actual fun getHomePath(): String = Environment.getExternalStorageDirectory().absolutePath;

    // 获取路径分隔符
    actual fun getPathSeparator(): String = File.separator

    // 获取根目录
    actual fun getRootPaths(): List<PathInfo> = File.listRoots()
        .map { PathInfo(it.path, it.totalSpace, it.freeSpace) }

    // 遍历目录
    actual fun traverse(path: String, callback: (List<FileSimpleInfo>) -> Unit) {
        val fileList = mutableListOf<FileSimpleInfo>()
        val directory = File(path)
        if (directory.exists()) {
            if (directory.isDirectory) {
                val files = directory.listFiles() ?: emptyArray()
                for (file in files) {
                    try {
                        fileList.add(file.toFileSimpleInfo())
                        if (file.isDirectory) {
                            traverse(file.path, callback)
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
        callback(fileList)
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