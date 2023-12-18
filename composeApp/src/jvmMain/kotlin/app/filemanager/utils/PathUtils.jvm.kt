package app.filemanager.utils

import java.io.File

// 获取用户目录
internal actual fun getHomePath(): String = System.getProperty("user.home")

// 获取路径分隔符
internal actual fun getPathSeparator(): String = File.separator

// 获取根目录
internal actual fun getRootPaths(): List<String> = File.listRoots().map { it.path }
