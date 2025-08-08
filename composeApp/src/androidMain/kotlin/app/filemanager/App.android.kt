package app.filemanager

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import app.filemanager.service.BackgroundService
import kotlinx.coroutines.DelicateCoroutinesApi
import java.io.File


class AndroidApp : Application() {
    companion object {
        lateinit var INSTANCE: AndroidApp
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }
}

class AppActivity : ComponentActivity() {
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Toast.makeText(baseContext, "获取是否是在首页", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startBackgroundService()
        } else {
            Toast.makeText(this, "需要通知权限以保持后台运行", Toast.LENGTH_LONG).show()
            // 即使没有通知权限也尝试启动服务
            startBackgroundService()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this@AppActivity
        
        if (!Environment.isExternalStorageManager()) {
            startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
        
        checkAndRequestNotificationPermission()
        
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) 需要运行时请求通知权限
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 权限已授予
                    startBackgroundService()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // 显示权限说明
                    Toast.makeText(this, "需要通知权限以显示后台运行状态", Toast.LENGTH_LONG).show()
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // 直接请求权限
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12 及以下版本不需要运行时权限
            startBackgroundService()
        }
    }

    private fun startBackgroundService() {
        BackgroundService.start(this)
    }

    companion object {
        var activity: AppActivity? = null
        fun openFile(path: String) {
            var mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(File(path).extension)
            if (mimeType == null) {
                mimeType = "application/octet-stream"
            }

            val uri = FileProvider.getUriForFile(activity!!, "app.filemanager.provider", File(path))
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity!!.startActivity(Intent.createChooser(intent, null))
        }
    }
}

internal actual fun openUrl(url: String?) {
    val uri = url?.let { Uri.parse(it) } ?: return
    val intent = Intent().apply {
        action = Intent.ACTION_VIEW
        data = uri
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    AndroidApp.INSTANCE.startActivity(intent)
}