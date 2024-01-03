package app.filemanager.ui.state.file

import androidx.compose.runtime.mutableStateListOf
import app.filemanager.db.FileFavorite
import app.filemanager.db.FileManagerDatabase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FileFavoriteState : KoinComponent {
    private val database by inject<FileManagerDatabase>()

    val favorites = mutableStateListOf<FileFavorite>()

    var startLimit = 0L
    var endLimit = 100L

    init {
        favorites.addAll(database.fileFavoriteQueries.queryAllByLimit(startLimit, endLimit).executeAsList())
    }

    fun updateFixed(favorite: FileFavorite){

    }

    fun delete(favorite: FileFavorite) {
        database.fileFavoriteQueries.deleteById(favorite.id)
        favorites.remove(favorite)
    }
}