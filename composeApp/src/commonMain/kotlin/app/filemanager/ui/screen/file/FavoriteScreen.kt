package app.filemanager.ui.screen.file

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
            floatingActionButton = {
                ExtendedFloatingActionButton({ }) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("新增")
                }
            }
        ) {

        }
    }
}