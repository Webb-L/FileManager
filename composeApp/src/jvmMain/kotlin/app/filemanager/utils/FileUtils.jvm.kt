package app.filemanager.utils

import java.awt.Desktop
import java.io.File

internal actual object FileUtils {
    actual fun openFile(file: String) {
        Desktop.getDesktop().open(File(file))
    }
}