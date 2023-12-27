package app.filemanager.extensions

import java.nio.file.Paths
import kotlin.io.path.name

internal actual fun String.parsePath(): List<String> = Paths.get(this).map { it.name }