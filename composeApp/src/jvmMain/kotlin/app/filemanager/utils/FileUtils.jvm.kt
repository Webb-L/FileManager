package app.filemanager.utils

import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

internal actual object FileUtils {
    actual fun openFile(file: String) {
        Desktop.getDesktop().open(File(file))
    }

    actual fun copyFile(src: String, dst: String) {
        val srcFile = File(src)
        val srcDir = Paths.get(src)
        val destFile = File(dst)
        val destDir = Paths.get(dst)
        if (srcFile.isDirectory) {
            Files.walk(srcDir).forEach { source ->
                val dest = destDir.resolve(srcDir.relativize(source))
                if (Files.isDirectory(source)) {
                    Files.createDirectories(dest)
                } else {
                    Files.copy(source, dest, StandardCopyOption.COPY_ATTRIBUTES)
                }
            }
        } else {
            if (srcFile.isFile && destFile.isFile) {
                Files.copy(srcDir, destDir, StandardCopyOption.COPY_ATTRIBUTES)
            } else {
                srcFile.copyTo(File(destFile, destFile.name))
            }
        }
    }

    actual fun moveFile(src: String, dst: String) {
    }

    actual fun deleteFile(path: String) {
        val file = File(path)
        if (file.isDirectory) {
            PathUtils.traverse(path)
        } else {
            file.delete()
        }
    }

    actual fun renameFile(path: String, name: String) {
    }

    actual fun totalSpace(path: String): Long = File(path).totalSpace
    actual fun freeSpace(path: String): Long = File(path).freeSpace
    actual fun createFolder(path: String)  = File(path).mkdir()
}