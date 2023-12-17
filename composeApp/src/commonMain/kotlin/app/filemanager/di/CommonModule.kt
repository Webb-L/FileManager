package app.filemanager.di

import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.MainState
import org.koin.dsl.module

val commonModule = module {
    single {
        MainState()
    }
    single {
        FileState()
    }
}