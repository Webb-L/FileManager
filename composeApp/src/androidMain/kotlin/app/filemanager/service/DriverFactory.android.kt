package app.filemanager.service

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.filemanager.AndroidApp
import app.filemanager.db.FileManagerDatabase

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(FileManagerDatabase.Schema, AndroidApp.INSTANCE, "file_manager.db")
    }
}