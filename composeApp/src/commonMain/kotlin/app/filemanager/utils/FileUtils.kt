package app.filemanager.utils

import app.filemanager.data.FileInfo

internal expect object FileUtils {
    fun getFile(path: String): FileInfo
    fun getFile(path: String, fileName: String): FileInfo
    fun openFile(file: String)
    fun copyFile(src: String, dest: String): Boolean
    fun moveFile(src: String, dest: String): Boolean
    fun deleteFile(path: String): Boolean
    fun renameFile(path: String, name: String)
    fun totalSpace(path: String): Long
    fun freeSpace(path: String): Long
    fun createFolder(path: String, name: String): Boolean
    fun renameFolder(path: String, oldName: String, newName: String): Boolean
}
