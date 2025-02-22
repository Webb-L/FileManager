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

    fun sync() {
        startLimit = 0L
        endLimit = 100L
        favorites.clear()
        favorites.addAll(database.fileFavoriteQueries.queryAllByLimit(startLimit, endLimit).executeAsList())
    }

    fun updateFixed(favorite: FileFavorite) {
        val index = favorites.indexOf(favorite)
        if (index < 0) return
        database.fileFavoriteQueries.updateIsFixedById(!favorite.isFixed, favorite.id)
        favorites[index] = favorite.copy(isFixed = !favorite.isFixed)
        favorites.sortByDescending { it.isFixed }
    }

    fun delete(favorite: FileFavorite) {
        database.fileFavoriteQueries.deleteById(favorite.id)
        favorites.remove(favorite)
    }
}