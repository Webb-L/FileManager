package app.filemanager.data.file

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable

enum class FileFilterIcon(type: String) {
    Text("Text"),
    Audio("Audio"),
    Video("Video"),
    Image("Image"),
    ImageRaw("ImageRaw"),
    ImageVector("ImageVector"),
    Image3D("Image3D"),
    PageLayout("PageLayout"),
    Database("Database"),
    Executable("Executable"),
    Game("Game"),
    CAD("CAD"),
    GIS("GIS"),
    Web("Web"),
    Plugin("Plugin"),
    Font("Font"),
    System("System"),
    Settings("Settings"),
    Encoded("Encoded"),
    Compressed("Compressed"),
    Disk("Disk"),
    Developer("Developer"),
    Backup("Backup"),
    Misc("Misc"),
    Custom("Custom")
}

@Composable
fun getFileFilterIcon(type: FileFilterIcon) {
    when (type) {
        FileFilterIcon.Text -> Icon(Icons.Default.Description, null)
        FileFilterIcon.Audio -> Icon(Icons.Default.Headphones, null)
        FileFilterIcon.Video -> Icon(Icons.Default.Videocam, null)
        FileFilterIcon.Image -> Icon(Icons.Default.Image, null)
        FileFilterIcon.ImageRaw -> Icon(Icons.Default.RawOn, null)
        FileFilterIcon.ImageVector -> Icon(Icons.Default.Landscape, null)
        FileFilterIcon.Image3D -> Icon(Icons.Default.ViewInAr, null)
        FileFilterIcon.PageLayout -> Icon(Icons.Default.Article, null)
        FileFilterIcon.Database -> Icon(Icons.Default.Dataset, null)
        FileFilterIcon.Executable -> Icon(Icons.Default.Emergency, null)
        FileFilterIcon.Game -> Icon(Icons.Default.Games, null)
        FileFilterIcon.CAD -> Icon(Icons.Default.Architecture, null)
        FileFilterIcon.GIS -> Icon(Icons.Default.Map, null)
        FileFilterIcon.Web -> Icon(Icons.Default.Web, null)
        FileFilterIcon.Plugin -> Icon(Icons.Default.Extension, null)
        FileFilterIcon.Font -> Icon(Icons.Default.FontDownload, null)
        FileFilterIcon.System -> Icon(Icons.Default.Build, null)
        FileFilterIcon.Settings -> Icon(Icons.Default.SettingsEthernet, null)
        FileFilterIcon.Encoded -> Icon(Icons.Default.Lock, null)
        FileFilterIcon.Compressed -> Icon(Icons.Default.FolderZip, null)
        FileFilterIcon.Disk -> Icon(Icons.Default.Adjust, null)
        FileFilterIcon.Developer -> Icon(Icons.Default.DataObject, null)
        FileFilterIcon.Backup -> Icon(Icons.Default.Backup, null)
        FileFilterIcon.Misc -> Icon(Icons.Default.HideSource, null)
        FileFilterIcon.Custom -> Icon(Icons.Default.Filter, null)
    }
}

data class FileFilter(
    val name: String,
    val iconType: FileFilterIcon,
    val isEnable: Boolean = true,
    val iconPath: String = ""
)
