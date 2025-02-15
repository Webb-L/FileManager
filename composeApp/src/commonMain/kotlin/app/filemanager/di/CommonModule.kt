package app.filemanager.di

import app.filemanager.createSettings
import app.filemanager.service.DriverFactory
import app.filemanager.service.createDatabase
import app.filemanager.ui.state.device.DeviceCertificateState
import app.filemanager.ui.state.device.DevicePermissionState
import app.filemanager.ui.state.device.DeviceRoleState
import app.filemanager.ui.state.device.DeviceSettingsState
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
    single { FileShareState() }
    single { DeviceRoleState(get()) }
    single { DevicePermissionState(get()) }
    single { DeviceSettingsState(get()) }
    single { DeviceCertificateState(get()) }
}

val commonDatabaseModule = module {
    single {
        val driverFactory = DriverFactory()
        createDatabase(driverFactory)
    }
}