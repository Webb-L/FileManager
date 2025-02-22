package app.filemanager.extensions

import com.eygraber.uri.Uri

internal actual fun String.parsePath(): List<String>  = Uri.parse(this).pathSegments