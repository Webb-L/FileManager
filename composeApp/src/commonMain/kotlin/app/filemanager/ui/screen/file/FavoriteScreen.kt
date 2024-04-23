package app.filemanager.ui.screen.file

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import app.filemanager.ui.components.FileFavoriteCard
import app.filemanager.ui.components.GridList
import app.filemanager.ui.state.file.FileFavoriteState
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.MainState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class FavoriteScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val scope = rememberCoroutineScope()

        val mainState = koinInject<MainState>()
        val fileFavoriteState = koinInject<FileFavoriteState>()
        fileFavoriteState.sync()

        val fileState = koinInject<FileState>()

        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("收藏") },
                    navigationIcon = {
                        IconButton({
                            mainState.updateFavorite(false)
                            navigator.pop()
                        }) {
                            Icon(Icons.Default.ArrowBack, null)
                        }
                    },
                    actions = {
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) {
            val favorites = fileFavoriteState.favorites
            GridList(
                isEmpty = favorites.isEmpty(),
                modifier = Modifier.padding(it)
            ) {
                items(favorites) { favorite ->
                    FileFavoriteCard(favorite = favorite,
                        onClick = {
                            scope.launch {
                                var path = favorite.path
                                if (!favorite.isDirectory) {
                                    path = path.replace(favorite.name, "")
                                }
                                fileState.updatePath(path)
                            }
                            mainState.updateFavorite(false)
                            navigator.pop()
                        },
                        onFixed = { fileFavoriteState.updateFixed(favorite) },
                        onRemove = { fileFavoriteState.delete(favorite) }
                    )
                }
            }
        }
    }
}