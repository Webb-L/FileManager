package app.filemanager

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.filemanager.di.appModule
import app.filemanager.ui.screen.main.MainScreen
import app.filemanager.ui.state.main.MainState
import app.filemanager.ui.theme.FileManagerTheme
import app.filemanager.utils.calculateWindowSizeClass
import org.koin.core.context.startKoin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun App() = FileManagerTheme {
    startKoin {
        modules(appModule())
    }
    val mainState = MainState()
    BoxWithConstraints(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        MainScreen(mainState, calculateWindowSizeClass(maxWidth, maxHeight))
    }
}

internal expect fun openUrl(url: String?)