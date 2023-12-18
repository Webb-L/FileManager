package app.filemanager.data.main

enum class DrawerBookmarkIcon {
    Favorite,
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
    val iconType: DrawerBookmarkIcon,
    val iconPath: String = ""
)

data class DrawerDevice(
    val name: String,
    val host: String,
    val username: String,
    val password: String
)

enum class DrawerNetworkProtocol {
    FTP,
    SFTP,
    SMB,

}

data class DrawerNetwork(
    val name: String,
    val protocol: String,
    val host: String,
    val username: String,
    val password: String
)

