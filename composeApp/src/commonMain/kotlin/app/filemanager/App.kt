package app.filemanager

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import app.filemanager.di.appModule
import app.filemanager.service.rpc.SocketClientIPEnum
import app.filemanager.service.rpc.getAllIPAddresses
import app.filemanager.ui.screen.main.MainScreen
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.ui.state.main.MainState
import app.filemanager.ui.theme.FileManagerTheme
import app.filemanager.utils.calculateWindowSizeClass
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

@Composable
internal fun App() = KoinApplication(application = {
    modules(appModule())
}) {
    val deviceState = koinInject<DeviceState>()
    val mainState = koinInject<MainState>()

    FileManagerTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            val windowSizeClass = calculateWindowSizeClass(maxWidth, maxHeight)
            mainState.windowSize = windowSizeClass
            MainScreen(windowSizeClass)
        }
    }

    LaunchedEffect(Unit) {
        deviceState.scanner(getAllIPAddresses(type = SocketClientIPEnum.IPV4_UP))
    }
}

internal expect fun openUrl(url: String?)