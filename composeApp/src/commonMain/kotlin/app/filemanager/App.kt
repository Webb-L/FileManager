package app.filemanager

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.filemanager.di.appModule
import app.filemanager.ui.screen.main.MainScreen
import app.filemanager.ui.theme.FileManagerTheme
import app.filemanager.utils.calculateWindowSizeClass
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.compose.KoinApplication

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
internal fun App() = KoinApplication(application = {
    modules(appModule())
}) {
    FileManagerTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            MainScreen(calculateWindowSizeClass(maxWidth, maxHeight))
        }
    }
}

internal expect fun openUrl(url: String?)