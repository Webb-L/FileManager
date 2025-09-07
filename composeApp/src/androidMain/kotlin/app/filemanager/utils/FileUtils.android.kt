package app.filemanager.utils

import app.filemanager.AppActivity
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.exception.AuthorityException
import app.filemanager.extensions.toFileSimpleInfo
import java.io.File
import java.io.RandomAccessFile

internal actual object FileUtils {
    actual fun getFile(path: String): Result<FileSimpleInfo> {
        val file = File(path)
        if (file.exists()) {
            return File(path).toFileSimpleInfo()
        }
        return Result.failure(Exception("找不到文件"))
    }
    actual fun getFile(path: String, fileName: String): Result<FileSimpleInfo> = File(path, fileName).toFileSimpleInfo()
    actual fun openFile(file: String) {
        AppActivity.openFile(file)
    }

    actual fun deleteFile(path: String): Result<Boolean> {
        val file = File(path)
        if (!file.exists()) {
            return Result.success(true)
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
    actual fun createFolder(path: String): Result<Boolean> {
        val file = File(path)
        if (file.exists()) return Result.success(true)
        return try {
            Result.success(file.mkdir())
        } catch (e: SecurityException) {
            Result.failure(AuthorityException("没有权限"))
        } catch (e: Exception) {
            Result.failure(AuthorityException(e.message))
        }
    }

    actual fun createFolder(path: String, name: String): Result<Boolean> {
        val file = File(path, name)
        if (file.exists()) return Result.success(true)
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
            if (start < 0 || end > fileLength || start > end) {
                return Result.failure(Exception("无效的范围"))
            }

            file.inputStream().use { input ->
                input.skip(start)
                val buffer = ByteArray((end - start).toInt())
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

            if (file.length() == 0.toLong()) {
                onChunkRead(Result.success(Pair(0, byteArrayOf())))
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

            if (fileSize == 0L) {
                return Result.success(true)
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

    actual fun createFile(path: String): Result<Boolean> {
        try {
            val file = File(path)

            if (!file.exists()) {
                file.createNewFile()
            }

            return Result.success(file.exists())
        } catch (e: Exception) {
            return Result.failure(Exception(e.message))
        }
    }

    actual fun createFile(path: String, name: String): Result<Boolean> {
        try {
            val file = File(path, name)

            if (!file.exists()) {
                file.createNewFile()
            }

            return Result.success(file.exists())
        } catch (e: Exception) {
            return Result.failure(Exception(e.message))
        }
    }
}