package app.filemanager.extensions

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import app.filemanager.data.file.FileSimpleInfo
import kotlinx.io.IOException
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes

fun File.toFileSimpleInfo(): Result<FileSimpleInfo> {
    val path = Paths.get(absolutePath)
    // 获取基本文件属性
    val attrs: BasicFileAttributes? = try {
        Files.readAttributes(path, BasicFileAttributes::class.java)
    } catch (e: NoSuchFileException) {
        println("文件不存在: ${e.message}")
        null
    } catch (e: AccessDeniedException) {
        println("访问被拒绝: ${e.message}")
        null
    } catch (e: IOException) {
        println("发生IO异常: ${e.message}")
        null
    } catch (e: Exception) {
        println("发生异常: ${e.message}")
        null
    }
    var mineType = ""
    if (isFile) {
        val extension = extension.toLowerCase(Locale.current)
        if (extension.isNotEmpty()) {
            mineType = ".$extension"
        }
    }

//    // 获取 POSIX 文件属性（如果支持）
//    val posixAttrs: PosixFileAttributes? = try {
//        Files.readAttributes(path, PosixFileAttributes::class.java)
//    } catch (e: Exception) {
//        null
//    }
//    println("path = $path")
//    println("逻辑大小: ${attrs?.size()} 字节 ${Files.size(path)}")
//    println("创建时间: ${attrs?.creationTime()}")
//    println("最后修改时间: ${attrs?.lastModifiedTime()}")
//    println("最后访问时间: ${attrs?.lastAccessTime()}")
//
//    // 打印权限信息（如果支持）
//    posixAttrs?.let {
//        val owner: UserPrincipal = it.owner()
//        val group: GroupPrincipal = it.group()
//        val permissions: Set<PosixFilePermission> = it.permissions()
//
//        println("文件拥有者: $owner")
//        println("文件所属组: $group")
//        println("权限: ${permissions.joinToString(", ") { it.name }}")
//    } ?: run {
//        println("不支持 POSIX 文件属性")
//    }

    return Result.success(
        FileSimpleInfo(
            name = name,
            description = "",
            isDirectory = isDirectory,
            isHidden = isHidden,
            path = absolutePath,
            mineType = mineType,
            // TODO 并不是实际文件实际大小
            size = if (isDirectory) (listFiles() ?: emptyArray<File>()).size.toLong() else attrs?.size() ?: length(),
            createdDate = attrs?.creationTime()?.toMillis() ?: 0,
            updatedDate = attrs?.lastModifiedTime()?.toMillis() ?: 0
        )
    )
}