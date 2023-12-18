package app.filemanager.utils

import java.io.File

internal actual object PathUtils {
    // 获取用户目录
    actual fun getHomePath(): String = System.getProperty("user.home")

    // 获取路径分隔符
    actual fun getPathSeparator(): String = File.separator

    // 获取根目录
    actual fun getRootPaths(): List<String> = File.listRoots().map { it.path }
}