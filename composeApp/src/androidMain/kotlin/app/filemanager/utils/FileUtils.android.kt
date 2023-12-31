package app.filemanager.utils

internal actual object FileUtils {
    actual fun openFile(file: String) {
    }

    actual fun copyFile(src: String, dest: String): Boolean {
        return false
    }

    actual fun moveFile(src: String, dest: String): Boolean {
        return false
    }

    actual fun deleteFile(path: String): Boolean {
        return false
    }

    actual fun renameFile(path: String, name: String) {
    }

    actual fun totalSpace(path: String): Long {
        return 1
    }

    actual fun freeSpace(path: String): Long {
        return 1
    }

    actual fun createFolder(path: String): Boolean {
        return false
    }
}