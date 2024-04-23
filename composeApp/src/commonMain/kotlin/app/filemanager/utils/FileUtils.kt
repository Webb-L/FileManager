package app.filemanager.utils

import app.filemanager.data.file.FileSimpleInfo

internal expect object FileUtils {
    fun getFile(path: String): FileSimpleInfo
    fun getFile(path: String, fileName: String): FileSimpleInfo
    fun openFile(file: String)
    fun copyFile(src: String, dest: String): Boolean
    fun moveFile(src: String, dest: String): Boolean
    fun deleteFile(path: String): Boolean
    fun totalSpace(path: String): Long
    fun freeSpace(path: String): Long
    fun createFolder(path: String, name: String): Boolean
    fun rename(path: String, oldName: String, newName: String): Boolean
    fun getData(filePath: String, start: Long, end: Long): ByteArray
}
