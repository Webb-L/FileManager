package app.filemanager

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import app.filemanager.di.appModule
import app.filemanager.service.WebSocketConnectService
import app.filemanager.ui.screen.main.MainScreen
import app.filemanager.ui.theme.FileManagerTheme
import app.filemanager.utils.calculateWindowSizeClass
import org.koin.compose.KoinApplication

@Composable
internal fun App() = KoinApplication(application = {
    modules(appModule())
}) {
    FileManagerTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            MainScreen(calculateWindowSizeClass(maxWidth, maxHeight))
        }
    }

    LaunchedEffect(Unit) {
        WebSocketConnectService().connect("127.0.0.1")
    }
}

internal expect fun openUrl(url: String?)