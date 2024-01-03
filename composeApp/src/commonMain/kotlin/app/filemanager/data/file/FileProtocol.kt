package app.filemanager.data.file

enum class FileProtocol(type: String) {
    Local("Local"),
    Device("Device"),
    Network("Network")
}