package app.filemanager

import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import java.security.Key
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


@OptIn(ExperimentalEncodingApi::class)
actual fun createSettings(): Settings {
    val context = AndroidApp.INSTANCE

    val preferences = EncryptedSharedPreferences.create(
        context,
        "encrypted_prefs",
        MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    val edit = preferences.edit()
    if (!preferences.contains("deviceName")) {
        edit.putString("deviceName", "${Build.MODEL}-Android")
        edit.apply()
    }
    if (!preferences.contains("deviceId")) {
        try {
            val secretKey: Key = SecretKeySpec("1234567890123456".toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedBytes = cipher.doFinal(UUID.randomUUID().toString().replace("-", "O").toByteArray())
            val deviceId = Base64.encode(encryptedBytes)
            edit.putString("deviceId", deviceId.replace("+", "Y").replace("/", "L"))
            edit.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return SharedPreferencesSettings(preferences)
}