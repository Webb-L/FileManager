package app.filemanager.data.main

import app.filemanager.data.file.DeviceType

enum class DrawerBookmarkType {
    Home,
    Image,
    Audio,
    Video,
    Document,
    Download,
    Custom
}

data class DrawerBookmark(
    val name: String,
    val path: String,
    val iconType: DrawerBookmarkType,
    val iconPath: String = ""
)

data class Device(
    val id: String,
    val name: String,
    val host: String,
    val type: DeviceType
)

enum class DrawerNetworkProtocol {
    FTP,
    SFTP,
    SMB,
    WebDav,
}

data class DrawerNetwork(
    val name: String,
    val protocol: String,
    val host: String,
    val username: String,
    val password: String
)

