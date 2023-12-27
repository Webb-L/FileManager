package app.filemanager.utils

internal expect object FileUtils {
    fun openFile(file: String)
    fun copyFile(src: String, dst: String)
    fun moveFile(src: String, dst: String)
    fun deleteFile(path: String)
    fun renameFile(path: String, name: String)
    fun totalSpace(path: String): Long
    fun freeSpace(path: String): Long
    fun createFolder(path: String): Boolean
}
