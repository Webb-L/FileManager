package app.filemanager.service

import app.cash.sqldelight.db.SqlDriver
import app.filemanager.db.FileManagerDatabase

expect class DriverFactory() {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): FileManagerDatabase {
    val driver = driverFactory.createDriver()
    val database = FileManagerDatabase(driver)

    // Do more work with the database (see below).
    return database
}