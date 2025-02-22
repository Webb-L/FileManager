package app.filemanager.data.file

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable

enum class FileFilterType(type: String) {
    Folder("Folder"),
    File("File"),
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
fun getFileFilterType(type: FileFilterType) {
    when (type) {
        FileFilterType.Folder -> Icon(Icons.Default.Folder, null)
        FileFilterType.File -> Icon(Icons.AutoMirrored.Default.Note, null)
        FileFilterType.Text -> Icon(Icons.Default.Description, null)
        FileFilterType.Audio -> Icon(Icons.Default.Headphones, null)
        FileFilterType.Video -> Icon(Icons.Default.Videocam, null)
        FileFilterType.Image -> Icon(Icons.Default.Image, null)
        FileFilterType.ImageRaw -> Icon(Icons.Default.RawOn, null)
        FileFilterType.ImageVector -> Icon(Icons.Default.Landscape, null)
        FileFilterType.Image3D -> Icon(Icons.Default.ViewInAr, null)
        FileFilterType.PageLayout -> Icon(Icons.AutoMirrored.Default.Article, null)
        FileFilterType.Database -> Icon(Icons.Default.Dataset, null)
        FileFilterType.Executable -> Icon(Icons.Default.Emergency, null)
        FileFilterType.Game -> Icon(Icons.Default.Games, null)
        FileFilterType.CAD -> Icon(Icons.Default.Architecture, null)
        FileFilterType.GIS -> Icon(Icons.Default.Map, null)
        FileFilterType.Web -> Icon(Icons.Default.Web, null)
        FileFilterType.Plugin -> Icon(Icons.Default.Extension, null)
        FileFilterType.Font -> Icon(Icons.Default.FontDownload, null)
        FileFilterType.System -> Icon(Icons.Default.Build, null)
        FileFilterType.Settings -> Icon(Icons.Default.SettingsEthernet, null)
        FileFilterType.Encoded -> Icon(Icons.Default.Lock, null)
        FileFilterType.Compressed -> Icon(Icons.Default.FolderZip, null)
        FileFilterType.Disk -> Icon(Icons.Default.Adjust, null)
        FileFilterType.Developer -> Icon(Icons.Default.DataObject, null)
        FileFilterType.Backup -> Icon(Icons.Default.Backup, null)
        FileFilterType.Misc -> Icon(Icons.Default.HideSource, null)
        FileFilterType.Custom -> Icon(Icons.Default.MoreHoriz, null)
    }
}

enum class FileFilterSort(type: Int) {
    NameAsc(0),
    NameDesc(1),
    SizeAsc(2),
    SizeDesc(3),
    TypeAsc(4),
    TypeDesc(5),
    CreatedDateAsc(6),
    CreatedDateDesc(7),
    UpdatedDateAsc(8),
    UpdatedDateDesc(9),
}