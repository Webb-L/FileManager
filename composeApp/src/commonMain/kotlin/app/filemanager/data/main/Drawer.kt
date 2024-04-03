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


abstract class DiskBase {
    abstract val name: String
}

data class Local(
    override val name: String = "本地"
) : DiskBase()

enum class DeviceType(type: String) {
    Android("Android"),
    IOS("IOS"),
    JVM("JVM"),
    JS("JS")
}

data class Device(
    val id: String,
    override val name: String,
    val host: String,
    val type: DeviceType
) : DiskBase()

enum class NetworkProtocol {
    FTP,
    SFTP,
    SMB,
    WebDav,
}

data class Network(
    override val name: String,
    val protocol: String,
    val host: String,
    val username: String,
    val password: String
) : DiskBase()
