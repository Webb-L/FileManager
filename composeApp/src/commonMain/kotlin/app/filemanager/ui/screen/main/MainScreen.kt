package app.filemanager.ui.screen.main

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.filemanager.ui.components.AppDrawer
import app.filemanager.ui.navigator.HomeNavigator
import app.filemanager.ui.state.main.MainState
import app.filemanager.utils.WindowSizeClass
import org.koin.compose.koinInject

@Composable
fun MainScreen(screenType: WindowSizeClass) {
    val mainState = koinInject<MainState>()
    val expandDrawer by mainState.isExpandDrawer.collectAsState()

    // 小屏
    if (screenType == WindowSizeClass.Compact) {
        ModalNavigationDrawer(
            drawerState = DrawerState(
                if (expandDrawer) DrawerValue.Open else DrawerValue.Closed,
                confirmStateChange = {
                    if (it == DrawerValue.Closed) {
                        mainState.updateExpandDrawer(false)
                    }
                    true
                },
            ),
            drawerContent = { AppDrawer() },
        ) {
            HomeNavigator()
        }
    } else {
        Row {
            if (listOf(WindowSizeClass.Medium, WindowSizeClass.Expanded).contains(screenType) && expandDrawer) {
                AppDrawer()
            }
            HomeNavigator()
        }
    }
}