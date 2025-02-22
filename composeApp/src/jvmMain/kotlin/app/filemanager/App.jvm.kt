package app.filemanager

import java.awt.Desktop
import java.io.InputStream
import java.net.URI

internal actual fun openUrl(url: String?) {
    val uri = url?.let { URI.create(it) } ?: return
    Desktop.getDesktop().browse(uri)
}

actual fun readResourceFile(path: String): ByteArray {
    val inputStream: InputStream = object {}.javaClass.classLoader.getResourceAsStream(path)
        ?: throw IllegalArgumentException("Resource not found: $path")
    return inputStream.readAllBytes()
}