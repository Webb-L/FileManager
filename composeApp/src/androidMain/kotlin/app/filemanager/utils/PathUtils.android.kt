package app.filemanager.utils

import android.os.Environment
import app.filemanager.AndroidApp
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.PathInfo
import app.filemanager.data.main.DrawerBookmark
import app.filemanager.data.main.DrawerBookmarkType
import app.filemanager.exception.AuthorityException
import app.filemanager.exception.EmptyDataException
import app.filemanager.extensions.toFileSimpleInfo
import io.github.aakira.napier.Napier
import java.io.File
import java.io.File.separator

internal actual object PathUtils {
    // 获取目录下所有文件和文件夹
    actual fun getFileAndFolder(path: String): Result<List<FileSimpleInfo>> {
        if (path.isEmpty()) return Result.failure(AuthorityException("目录错误"))
        val file = File(path)
        if (!file.exists()) return Result.failure(AuthorityException("该目录并不存在"))
        if (!file.canRead()) return Result.failure(AuthorityException("没有权限"))

        val listFiles = file.listFiles() ?: return Result.failure(EmptyDataException())
        if (listFiles.isEmpty()) return Result.failure(EmptyDataException())

        return Result.success(listFiles.mapNotNull { file ->
            try {
                file.toFileSimpleInfo().getOrNull()
            } catch (e: Exception) {
                null
            }
        })
    }

    // 获取用户目录
    actual fun getAppPath(): String = AndroidApp.INSTANCE.filesDir.absolutePath;

    // 获取用户目录
    actual fun getHomePath(): String = Environment.getExternalStorageDirectory().absolutePath;

    // 获取应用缓存目录
    actual fun getCachePath(): String = AndroidApp.INSTANCE.cacheDir.absolutePath

    // 获取路径分隔符
    actual fun getPathSeparator(): String = separator

    // 获取根目录
    actual fun getRootPaths(): Result<List<PathInfo>> {
        return try {
            Result.success(
                File.listRoots()
                    .map { PathInfo(it.path, it.totalSpace, it.freeSpace) })
        } catch (e: SecurityException) {
            Result.failure(AuthorityException("没有权限访问目录"))
        } catch (e: Exception) {
            Result.failure(Exception())
        }
    }

    // 遍历目录
    actual fun traverse(path: String, callback: (Result<List<FileSimpleInfo>>) -> Unit) {
        if (path.isEmpty()) {
            callback(Result.failure(AuthorityException("路径错误")))
            return
        }

        val directory = File(path)
        if (!directory.exists()) {
            callback(Result.failure(AuthorityException("该目录并不存在")))
            return
        }

        if (!directory.canRead()) {
            callback(Result.failure(AuthorityException("没有权限读取该目录")))
            return
        }

        try {
            val fileList = mutableListOf<FileSimpleInfo>()
            if (directory.isDirectory) {
                val files = directory.listFiles() ?: throw EmptyDataException()
                for (file in files) {
                    try {
                        fileList.add(file.toFileSimpleInfo().getOrNull()!!)
                        if (file.isDirectory) {
                            traverse(file.path) { result ->
                                result.fold(
                                    onSuccess = {
                                        fileList.addAll(it)
                                    },
                                    onFailure = {
                                        callback(Result.failure(it))
                                    }
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Napier.e { "文件处理失败：${e.message}" }
                    }
                }
            } else {
                fileList.add(directory.toFileSimpleInfo().getOrNull()!!)
            }

            callback(Result.success(fileList))
        } catch (e: SecurityException) {
            callback(Result.failure(AuthorityException("没有权限访问目录")))
        } catch (e: Exception) {
            callback(Result.failure(Exception("未知错误：${e.message}")))
        }
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
            DrawerBookmark(
                name = "音乐",
                path = "$homePath${separator}Music",
                iconType = DrawerBookmarkType.Audio
            ),
            DrawerBookmark(
                name = "视频",
                path = "$homePath${separator}Movies",
                iconType = DrawerBookmarkType.Video
            ),
            DrawerBookmark(
                name = "文档",
                path = "$homePath${separator}Documents",
                iconType = DrawerBookmarkType.Document
            ),
            DrawerBookmark(
                name = "下载",
                path = "$homePath${separator}Download",
                iconType = DrawerBookmarkType.Download
            ),
        )
    }
}