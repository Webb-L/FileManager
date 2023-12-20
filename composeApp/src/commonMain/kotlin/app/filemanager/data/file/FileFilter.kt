package app.filemanager.data.file

enum class FileFilterIcon {
    Text,
    Audio,
    Video,
    Image,
    ImageRaw,
    ImageVector,
    Image3D,
    PageLayout,
    Database,
    Executable,
    Game,
    CAD,
    GIS,
    Web,
    Plugin,
    Font,
    System,
    Settings,
    Encoded,
    Compressed,
    Disk,
    Developer,
    Backup,
    Misc,
    Custom
}

data class FileFilter(
    val name: String,
    val extensions: List<String>,
    val iconType: FileFilterIcon,
    val isEnable: Boolean = true,
    val iconPath: String = ""
)
