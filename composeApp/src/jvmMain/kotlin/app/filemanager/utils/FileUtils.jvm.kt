package app.filemanager.utils

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.exception.AuthorityException
import app.filemanager.extensions.toFileSimpleInfo
import io.github.aakira.napier.Napier
import java.awt.Desktop
import java.io.File
import java.io.RandomAccessFile

internal actual object FileUtils {
    actual fun getFile(path: String): FileSimpleInfo = File(path).toFileSimpleInfo()
    actual fun getFile(path: String, fileName: String): FileSimpleInfo = File(path, fileName).toFileSimpleInfo()
    actual fun openFile(file: String) = Desktop.getDesktop().open(File(file))

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

    actual fun deleteFile(path: String): Result<Boolean> {
        val file = File(path)
        if (!file.exists()) {
            return Result.failure(Exception("文件不存在，无法删除"))
        }
        return try {
            if (file.delete()) {
                Result.success(true)
            } else {
                Result.failure(Exception("删除失败"))
            }
        } catch (e: SecurityException) {
            Result.failure(AuthorityException("没有权限"))
        } catch (e: Exception) {
            Result.failure(Exception(e.message))
        }
    }

    actual fun totalSpace(path: String): Long = File(path).totalSpace
    actual fun freeSpace(path: String): Long = File(path).freeSpace
    actual fun createFolder(path: String, name: String): Result<Boolean> {
        val file = File(path, name)
        if (file.exists()) return Result.failure(Exception("已经存在，无法创建"))
        return try {
            Result.success(file.mkdir())
        } catch (e: SecurityException) {
            Result.failure(AuthorityException("没有权限"))
        } catch (e: Exception) {
            Result.failure(AuthorityException(e.message))
        }
    }

    actual fun rename(path: String, oldName: String, newName: String): Result<Boolean> {
        val oldFile = File(path, oldName)
        val newFile = File(path, newName)

        if (!oldFile.exists()) return Result.failure(Exception("原文件不存在，无法重命名"))
        if (newFile.exists()) return Result.failure(Exception("目标文件名已存在，无法重命名"))

        return try {
            if (oldFile.renameTo(newFile)) {
                Result.success(true)
            } else {
                Result.failure(Exception("重命名失败"))
            }
        } catch (e: SecurityException) {
            Result.failure(AuthorityException("没有权限"))
        } catch (e: Exception) {
            Result.failure(Exception(e.message))
        }
    }

    actual fun readFile(path: String): Result<ByteArray> {
        return try {
            val file = File(path)
            if (!file.exists()) {
                Result.failure(Exception("文件不存在"))
            } else if (!file.canRead()) {
                Result.failure(AuthorityException("没有权限读取该文件"))
            } else {
                Result.success(file.readBytes())
            }
        } catch (e: SecurityException) {
            Result.failure(AuthorityException("没有权限"))
        } catch (e: Exception) {
            Result.failure(Exception(e.message))
        }
    }

    actual fun readFileRange(path: String, start: Long, end: Long): Result<ByteArray> {
        try {
            val file = File(path)
            if (!file.exists()) {
                return Result.failure(Exception("文件不存在"))
            }
            if (!file.canRead()) {
                return Result.failure(AuthorityException("没有权限读取该文件"))
            }

            val fileLength = file.length()
            if (start < 0 || end >= fileLength || start > end) {
                return Result.failure(Exception("无效的范围"))
            }

            file.inputStream().use { input ->
                input.skip(start)
                val buffer = ByteArray((end - start + 1).toInt())
                val bytesRead = input.read(buffer)
                return Result.success(buffer.copyOf(bytesRead))
            }
        } catch (e: SecurityException) {
            return Result.failure(AuthorityException("没有权限"))
        } catch (e: Exception) {
            return Result.failure(Exception(e.message))
        }
    }

    actual fun readFileChunks(
        path: String,
        chunkSize: Long,
        onChunkRead: (Result<Pair<Long, ByteArray>>) -> Unit
    ) {
        try {
            val file = File(path)
            if (!file.exists()) {
                onChunkRead(Result.failure(Exception("文件不存在")))
                return
            }
            if (!file.canRead()) {
                onChunkRead(Result.failure(AuthorityException("没有权限读取该文件")))
                return
            }
            if (chunkSize <= 0) {
                onChunkRead(Result.failure(Exception("无效的块大小")))
                return
            }

            file.inputStream().use { input ->
                val buffer = ByteArray(chunkSize.toInt())
                var currentIndex = 0L
                do {
                    val bytesRead = input.read(buffer)
                    if (bytesRead > 0) {
                        onChunkRead(
                            Result.success(
                                Pair(currentIndex, buffer.copyOf(bytesRead))
                            )
                        )
                        currentIndex++
                    }
                } while (bytesRead != -1)
            }
        } catch (e: SecurityException) {
            onChunkRead(Result.failure(AuthorityException("没有权限")))
        } catch (e: Exception) {
            onChunkRead(Result.failure(Exception(e.message)))
        }
    }

    actual fun writeBytes(
        path: String,
        fileSize: Long,
        data: ByteArray,
        offset: Long
    ): Result<Boolean> {
        try {
            val file = File(path)

            if (!file.exists()) {
                file.createNewFile()
            }

            if (!file.canWrite()) {
                return Result.failure(AuthorityException("没有权限写入文件"))
            }

            RandomAccessFile(file, "rw").use { raf ->
                if (raf.length() < fileSize) {
                    raf.setLength(fileSize)
                }

                raf.seek(offset)
                raf.write(data)
            }

            return Result.success(true)
        } catch (e: SecurityException) {
            return Result.failure(AuthorityException("没有权限写入文件"))
        } catch (e: Exception) {
            return Result.failure(Exception(e.message))
        }
    }
}