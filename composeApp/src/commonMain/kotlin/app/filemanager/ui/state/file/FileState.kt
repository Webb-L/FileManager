package app.filemanager.ui.state.file

import app.filemanager.ui.state.main.MainState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FileState(): KoinComponent {
    val mainState:MainState by inject()
}