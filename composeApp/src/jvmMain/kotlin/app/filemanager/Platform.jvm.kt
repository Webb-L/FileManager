package app.filemanager

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor
import java.util.*
import java.util.prefs.Preferences


actual fun createSettings(): Settings {
    val encryptor = StandardPBEStringEncryptor()
    encryptor.setPasswordCharArray("your password".toCharArray())
    val preferences = Preferences.userRoot()

    if (!preferences.nodeExists("deviceName")) {
        preferences.put("deviceName", "${System.getProperty("user.name")}-${System.getProperty("os.name")}")
    }
    if (!preferences.nodeExists("deviceId")) {
        val deviceId = encryptor.encrypt(UUID.randomUUID().toString().replace("-", "O"))
        preferences.put("deviceId", deviceId.replace("+", "Y").replace("/", "L"))
    }
    return PreferencesSettings(preferences)
}