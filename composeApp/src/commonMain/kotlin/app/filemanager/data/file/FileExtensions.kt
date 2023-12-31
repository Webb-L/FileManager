package app.filemanager.data.file

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import kotlinx.serialization.json.Json
import okio.internal.commonToUtf8String
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource

// https://wwwianaorg/assignments/media-types/media-typesxhtm
// https://fileinfocom/filetypes/
@OptIn(ExperimentalResourceApi::class)
object FileExtensions {
    private var extensionMap = mapOf<String, List<String>>()

    suspend fun init() {
        val readBytes = resource("config/file-extensions.json").readBytes()
        val tempMap = mutableMapOf<String, List<String>>()
        for (entry in Json.decodeFromString<Map<String, List<String>>>(readBytes.commonToUtf8String())) {
            tempMap[entry.key] = convertFileExtensions(entry.value)
        }
        extensionMap = tempMap
    }

    fun getExtensions(type: FileFilterType): List<String> {
        return extensionMap[type.name] ?: listOf()
    }

    fun getExtensionTypeByFileExtension(type: String): String {
        val filterValues = extensionMap.filterValues { it.contains(type) }
        if (filterValues.isEmpty()) {
            return ""
        }
        return filterValues.keys.last()
    }

    fun convertFileExtensions(fileExtensions: List<String>): List<String> {
        val convertedExtensions = mutableListOf<String>()
        for (extension in fileExtensions) {
            val convertedExtension = extension.toLowerCase(Locale.current)
            convertedExtensions.add(convertedExtension)
        }
        return convertedExtensions
    }
}