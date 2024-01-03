package app.filemanager.ui.screen.file

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.filemanager.ui.components.FileFavoriteCard
import app.filemanager.ui.components.NullDataError
import app.filemanager.ui.state.file.FileFavoriteState
import app.filemanager.ui.state.main.MainState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

class FavoriteScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val mainState = koinInject<MainState>()
        val fileFavoriteState = koinInject<FileFavoriteState>()

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
            if (fileFavoriteState.favorites.isEmpty()) {
                NullDataError()
                return@Scaffold
            }
            LazyColumn(Modifier.padding(it)) {
                items(fileFavoriteState.favorites) { favorite ->
                    FileFavoriteCard(favorite = favorite,
                        onClick = {
                            mainState.updatePath(favorite.path)
                            mainState.updateFavorite(false)
                            navigator.pop()
                        },
                        onRemove = { fileFavoriteState.delete(favorite) }
                    )
                }
            }
        }
    }
}