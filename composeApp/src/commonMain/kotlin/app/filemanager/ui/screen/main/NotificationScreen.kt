package app.filemanager.ui.screen.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import app.filemanager.ui.components.GridList
import app.filemanager.ui.state.file.FileFavoriteState
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.MainState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

class NotificationScreen : Screen {
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
                    title = { Text("通知") },
                    navigationIcon = {
                        IconButton({
                            navigator.pop()
                        }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                        }
                    },
                    actions = {
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) {
            GridList(modifier = Modifier.padding(it)) {
                items(30){
                    // Hardcoded example data
                    val category = "Promotions"
                    val title = "Exclusive Offer!"
                    val description = "Get 50% off on your next purchase. Limited time only!"
                    val imageUrl = "https://via.placeholder.com/150"

                    ListItem(
                        overlineContent = {
                            Text(category)
                        },
                        headlineContent = {
                            Text(title)
                        },
                        supportingContent = {
                            Column {
                                Text(description)
                                Row {
                                    TextButton({}){
                                        Text("More")
                                    }
                                    TextButton({}){
                                        Text("More")
                                    }
                                    TextButton({}){
                                        Text("More")
                                    }
                                }
                            }
                        },
                        leadingContent = {
//                            Icon(
//                                Icons.AutoMirrored.Default.WrapText,
//                                contentDescription = "Localized description"
//                            )
                        },
                    )
                }
            }
        }
    }
}