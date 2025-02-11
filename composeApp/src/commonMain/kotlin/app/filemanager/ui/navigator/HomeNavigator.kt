package app.filemanager.ui.navigator

import androidx.compose.runtime.Composable
import app.filemanager.ui.screen.device.DevicePermissionScreen
import app.filemanager.ui.screen.file.filter.FileFilterManagerScreen
import cafe.adriel.voyager.navigator.Navigator

@Composable
fun HomeNavigator() {
    Navigator(
        screen = DevicePermissionScreen(),
        onBackPressed = { currentScreen ->
            // TODO 代码不生效
            println(currentScreen)
            when (currentScreen) {
                is FileFilterManagerScreen -> {
                    println(12331321)
                }
            }
            false
        }
    )
}