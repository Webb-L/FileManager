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

