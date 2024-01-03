package app.filemanager.utils

import app.filemanager.data.FileInfo
import app.filemanager.data.file.FileFilter

object VerificationUtils {
    fun folder(text: String, fileInfos: List<FileInfo>, ignoreName: List<String> = emptyList()): Pair<Boolean, String> {
        val nameRegex = "[\\\\/:*?\"<>|]".toRegex()
        val filter = fileInfos.filter { if (ignoreName.contains(it.name)) false else it.name == text }
        return if (filter.isNotEmpty()) {
            Pair(true, "“$text”，已存在了。换个名称试试吧！")
        } else if (nameRegex.find(text) != null) {
            Pair(true, "名字不能包含有“\\\\/:*?\"<>|”这些符号。")
        } else {
            Pair(false, "")
        }
    }

    fun filterType(text: String, filterFileTypes: List<FileFilter>): Pair<Boolean, String> {
        val filter = filterFileTypes.filter { it.name == text }
        return if (filter.isNotEmpty()) {
            Pair(true, "“$text”，已存在了。换个名称试试吧！")
        } else {
            Pair(false, "")
        }
    }
}