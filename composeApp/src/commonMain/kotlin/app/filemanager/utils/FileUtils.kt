package app.filemanager.utils

internal expect object FileUtils {
    fun openFile(file: String)
    fun copyFile(src: String, dest: String): Boolean
    fun moveFile(src: String, dest: String): Boolean
    fun deleteFile(path: String): Boolean
    fun renameFile(path: String, name: String)
    fun totalSpace(path: String): Long
    fun freeSpace(path: String): Long
    fun createFolder(path: String): Boolean
}
