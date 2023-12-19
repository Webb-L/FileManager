package app.filemanager.data.file

enum class FileFilterIcon {
    Image,
    Audio,
    Video,
    Document,
    Custom
}

data class FileFilter(
    val name: String,
    val extensions: List<String>,
    val iconType: FileFilterIcon,
    val iconPath: String = ""
)
