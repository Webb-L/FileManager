package app.filemanager

import app.filemanager.data.main.DeviceType
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor
import java.util.*
import java.util.prefs.Preferences


actual fun createSettings(): Settings {
    val encryptor = StandardPBEStringEncryptor()
    encryptor.setPasswordCharArray("your password".toCharArray())
    val preferences = Preferences.userRoot()

    if (preferences.get("deviceName","").isEmpty()) {
        preferences.put("deviceName", "${System.getProperty("user.name")}-${System.getProperty("os.name")}")
    }
    if (preferences.get("deviceId","").isEmpty()) {
        val deviceId = encryptor.encrypt(UUID.randomUUID().toString().replace("-", "O"))
        preferences.put("deviceId", deviceId.replace("+", "Y").replace("/", "L"))
    }
    return PreferencesSettings(preferences)
}

actual val PlatformType: DeviceType = DeviceType.JVM