package app.filemanager.data.file

// https://www.iana.org/assignments/media-types/media-types.xhtm
// https://fileinfo.com/filetypes/
object FileExtensions {
    val Images = listOf("jpeg", "png", "gif", "bmp", "webp", "midi")
    val Audios = listOf("mp3", "wav", "acc", "ogg", "flac", "midi")
    val Videos = listOf("mp4", "avi", "mov", "wmv", "flv", "mkv")
    val Descriptions = listOf("pdf", "docx", "xlsx", "pptx", "odt", "csv")
}