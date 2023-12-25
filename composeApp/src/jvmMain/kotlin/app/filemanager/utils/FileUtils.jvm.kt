package app.filemanager.utils

import java.awt.Desktop
import java.io.File

internal actual object FileUtils {
    actual fun openFile(file: String) {
        Desktop.getDesktop().open(File(file))
    }

    actual fun copyFile(dst: String, src: String) {
    }

    actual fun totalSpace(path: String): Long = File(path).totalSpace
}