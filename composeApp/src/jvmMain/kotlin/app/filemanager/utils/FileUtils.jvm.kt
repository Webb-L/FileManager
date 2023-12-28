package app.filemanager.utils

import io.github.aakira.napier.Napier
import java.awt.Desktop
import java.io.File

internal actual object FileUtils {
    actual fun openFile(file: String) {
        Desktop.getDesktop().open(File(file))
    }

    actual fun copyFile(src: String, dst: String): Boolean {
        // TODO 复制文件夹只会复制文件夹下的内容。
        try {
            val srcFile = File(src)
            val destFile = File(dst)
            if (srcFile.isDirectory) {
                if (!destFile.exists()) {
                    return destFile.mkdirs()
                }
                return true
            } else {
                return srcFile.copyTo(destFile).exists()
            }
        } catch (e: Exception) {
            Napier.e { e.message.toString() }
            return false
        }

//        if (srcFile.isDirectory) {
//            Files.walk(srcDir).forEach { source ->
//                val dest = destDir.resolve(srcDir.relativize(source))
//                if (Files.isDirectory(source)) {
//                    Files.createDirectories(dest)
//                } else {
//                    Files.copy(source, dest, StandardCopyOption.COPY_ATTRIBUTES)
//                }
//            }
//        } else {
//            if (srcFile.isFile && destFile.isFile) {
//                Files.copy(srcDir, destDir, StandardCopyOption.COPY_ATTRIBUTES)
//            } else {
//                srcFile.copyTo(File(destFile, destFile.name))
//            }
//        }
    }

    actual fun moveFile(src: String, dst: String) {
    }

    actual fun deleteFile(path: String) = File(path).delete()

    actual fun renameFile(path: String, name: String) {
    }

    actual fun totalSpace(path: String): Long = File(path).totalSpace
    actual fun freeSpace(path: String): Long = File(path).freeSpace
    actual fun createFolder(path: String) = File(path).mkdir()
}