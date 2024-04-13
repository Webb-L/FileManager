package app.filemanager.di

import app.filemanager.createSettings
import app.filemanager.service.DriverFactory
import app.filemanager.service.WebSocketConnectService
import app.filemanager.service.createDatabase
import app.filemanager.ui.state.file.FileFavoriteState
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.ui.state.file.FileOperationState
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.ui.state.main.DrawerState
import app.filemanager.ui.state.main.MainState
import app.filemanager.ui.state.main.NetworkState
import org.koin.dsl.module

val commonModule = module {
    single { createSettings() }
}

val commonScreenModule = module {
    single { MainState() }
    single { DrawerState() }
    single { DeviceState() }
    single { NetworkState() }
    single { FileState() }
    single { FileFilterState() }
    single { FileOperationState() }
    single { FileFavoriteState() }
}

val commonDatabaseModule = module {
    single {
        val driverFactory = DriverFactory()
        createDatabase(driverFactory)
    }
}