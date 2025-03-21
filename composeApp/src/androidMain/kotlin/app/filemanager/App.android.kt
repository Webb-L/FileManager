package app.filemanager

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.FileProvider
import app.filemanager.service.rpc.startRpcServer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream


class AndroidApp : Application() {
    companion object {
        lateinit var INSTANCE: AndroidApp
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        GlobalScope.launch(Dispatchers.IO) {
            startRpcServer()
        }
    }
}

class AppActivity : ComponentActivity() {
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Toast.makeText(baseContext, "获取是否是在首页", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this@AppActivity
//        startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        enableEdgeToEdge()
        setContent {
            App()
        }
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

actual fun readResourceFile(path: String): ByteArray {
    val inputStream: InputStream = object {}.javaClass.classLoader.getResourceAsStream(path)
        ?: throw IllegalArgumentException("Resource not found: $path")
    return inputStream.readBytes()
}