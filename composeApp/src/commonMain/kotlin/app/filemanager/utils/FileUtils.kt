package app.filemanager.utils

internal expect object FileUtils {
    fun openFile(file: String)

    fun copyFile(dst: String, src: String)
}
