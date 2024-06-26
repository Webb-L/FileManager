package app.filemanager.service

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo

typealias WebSocketResultMapListFileSimpleInfo = Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>