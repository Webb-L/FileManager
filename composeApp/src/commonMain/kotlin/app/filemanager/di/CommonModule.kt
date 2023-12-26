package app.filemanager.di

import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DrawerState
import app.filemanager.ui.state.main.MainState
import org.koin.dsl.module

val commonModule = module {
    single {
        MainState()
    }
    single {
        DrawerState()
    }
    single {
        FileState()
    }
    single {
        FileFilterState()
    }
}