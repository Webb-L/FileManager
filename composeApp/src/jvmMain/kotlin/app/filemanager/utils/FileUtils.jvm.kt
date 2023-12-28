package app.filemanager.utils

import io.github.aakira.napier.Napier
import java.awt.Desktop
import java.io.File

internal actual object FileUtils {
    actual fun openFile(file: String) {
        Desktop.getDesktop().open(File(file))
    }

    actual fun copyFile(src: String, dest: String): Boolean {
        try {
            val srcFile = File(src)
            val destFile = File(dest)

            println("$srcFile $destFile")
            println("${srcFile.isDirectory} ${destFile.isDirectory}")

            if (srcFile.isDirectory && !destFile.exists()) {
                return destFile.mkdirs()
            }

            if (srcFile.isDirectory && destFile.isDirectory) {
                return File(destFile, srcFile.name).mkdir()
            }

            // 文件复制到文件夹
            if (srcFile.isFile && destFile.isDirectory) {
                return srcFile.copyTo(File(destFile, srcFile.name)).exists()
            }

            // 文件复制到文件
            if (srcFile.isFile) {
                return srcFile.copyTo(destFile).exists()
            }
//            if (srcFile.isFile && destFile.isDirectory) {
//                return srcFile.copyTo(File(destFile, srcFile.name)).exists()
//            }
//            // 文件夹复制到文件夹
//            if (srcFile.isDirectory && destFile.isDirectory) {
//                return false
//            }

//            if (srcFile.isDirectory) {
//                if (!destFile.exists()) {
//                    return destFile.mkdirs()
//                }
//                return true
//            } else {
//                println(srcFile)
//                println(destFile)
//                return srcFile.copyTo(destFile).exists()
//            }
        } catch (e: Exception) {
            Napier.e { e.message.toString() }
            return false
        }
        return false
    }

    // TODO 复制文件夹只会复制文件夹下的内容 /test/->/tdd/ 只会把/test/下所有文件复制到/tdd/
    actual fun moveFile(src: String, dest: String): Boolean {
        try {
            val srcFile = File(src)
            val destFile = File(dest)

            println("$srcFile $destFile")
            println("${srcFile.isDirectory} ${destFile.isDirectory}")

            if (srcFile.isDirectory && !destFile.exists()) {
                return destFile.mkdirs()
            }

            if (srcFile.isDirectory && destFile.isDirectory) {
                return File(destFile, srcFile.name).mkdir()
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

    actual fun deleteFile(path: String) = File(path).delete()

    actual fun renameFile(path: String, name: String) {
    }

    actual fun totalSpace(path: String): Long = File(path).totalSpace
    actual fun freeSpace(path: String): Long = File(path).freeSpace
    actual fun createFolder(path: String) = File(path).mkdir()
}