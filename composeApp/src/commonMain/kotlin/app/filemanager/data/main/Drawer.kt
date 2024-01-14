package app.filemanager.data.main

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

enum class DeviceType(type: String) {
    Android("Android"),
    IOS("IOS"),
    JVM("JVM"),
    JS("JS")
}

data class Device(
    val id: String,
    val name: String,
    val host: String,
    val type: DeviceType
)

enum class NetworkProtocol {
    FTP,
    SFTP,
    SMB,
    WebDav,
}

data class Network(
    val name: String,
    val protocol: String,
    val host: String,
    val username: String,
    val password: String
)

