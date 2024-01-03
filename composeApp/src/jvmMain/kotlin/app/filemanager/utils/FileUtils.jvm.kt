package app.filemanager.utils

import app.filemanager.data.file.FileInfo
import app.filemanager.extensions.toFileInfo
import io.github.aakira.napier.Napier
import java.awt.Desktop
import java.io.File

internal actual object FileUtils {
    actual fun getFile(path: String): FileInfo = File(path).toFileInfo()
    actual fun getFile(path: String, fileName: String): FileInfo = File(path, fileName).toFileInfo()

    actual fun openFile(file: String) {
        Desktop.getDesktop().open(File(file))
    }

    actual fun copyFile(src: String, dest: String): Boolean {
        try {
            val srcFile = File(src)
            val destFile = File(dest)
            println("${srcFile.path} ${destFile.path}")

            if (srcFile.isDirectory && !destFile.exists()) {
                return destFile.mkdirs()
            }

            if (srcFile.isDirectory && destFile.isDirectory) {
                destFile.mkdir()
                return destFile.exists()
            }

            // 文件复制到文件夹
            if (srcFile.isFile && destFile.isDirectory) {
                return srcFile.copyTo(File(destFile, srcFile.name)).exists()
            }

            // dest文件存在就删除该文件
            if (destFile.isFile && destFile.exists()) {
                destFile.delete()
            }

            // 文件复制到文件
            if (srcFile.isFile) {
                return srcFile.copyTo(destFile).exists()
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    // TODO 复制文件夹只会复制文件夹下的内容 /test/->/tdd/ 只会把/test/下所有文件复制到/tdd/
    actual fun moveFile(src: String, dest: String): Boolean {
        try {
            val srcFile = File(src)
            val destFile = File(dest)

            println("$srcFile $destFile ${srcFile.isDirectory} ${destFile.isDirectory}")
            if (srcFile.isDirectory && !destFile.exists()) {
                return srcFile.renameTo(destFile)
            }

            if (srcFile.isDirectory && destFile.isDirectory) {
                return srcFile.renameTo(destFile)
            }

            // 文件复制到文件夹
            if (srcFile.isFile && destFile.isDirectory) {
                return srcFile.renameTo(File(destFile, srcFile.name))
            }

            // 文件复制到文件
            if (srcFile.isFile) {
                return srcFile.renameTo(destFile)
            }
        } catch (e: Exception) {
            Napier.e { e.message.toString() }
            return false
        }
        return false
    }

    actual fun deleteFile(path: String): Boolean {
        val file = File(path)
        if (file.exists()) {
            return file.delete()
        }
        return true
    }

    actual fun renameFile(path: String, name: String) {
    }

    actual fun totalSpace(path: String): Long = File(path).totalSpace
    actual fun freeSpace(path: String): Long = File(path).freeSpace
    actual fun createFolder(path: String, name: String) = File(path, name).mkdir()
    actual fun renameFolder(path: String, oldName: String, newName: String): Boolean {
        val oldFile = File(path, oldName)
        val newFile = File(path, newName)
        return oldFile.renameTo(newFile)
    }
}