package app.filemanager.di

import app.filemanager.createSettings
import app.filemanager.service.DriverFactory
import app.filemanager.service.createDatabase
import app.filemanager.ui.state.file.*
import app.filemanager.ui.state.main.*
import org.koin.dsl.module

val commonModule = module {
    single { createSettings() }
}

val commonScreenModule = module {
    single { MainState() }
    single { TaskState() }
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