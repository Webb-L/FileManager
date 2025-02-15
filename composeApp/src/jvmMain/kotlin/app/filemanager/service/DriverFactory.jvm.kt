package app.filemanager.service

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.filemanager.db.FileManagerDatabase
import java.io.File


actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        // 指定数据库文件的路径
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:mydatabase.db")
        if (!File("mydatabase.db").exists()) {
            FileManagerDatabase.Schema.create(driver)
        }
        return driver
    }
}