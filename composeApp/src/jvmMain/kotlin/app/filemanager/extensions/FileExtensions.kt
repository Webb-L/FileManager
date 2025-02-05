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

    return Result.success(
        FileSimpleInfo(
            name = name,
            description = "",
            isDirectory = isDirectory,
            isHidden = isHidden,
            path = absolutePath,
            mineType = mineType,
            // TODO 并不是实际文件实际大小
            size = if (isDirectory) (listFiles() ?: emptyArray<File>()).size.toLong() else length(),
            createdDate = attrs?.creationTime()?.toMillis() ?: 0,
            updatedDate = attrs?.lastModifiedTime()?.toMillis() ?: 0
        )
    )
}