package app.filemanager

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import java.io.File


class AndroidApp : Application() {
    companion object {
        lateinit var INSTANCE: AndroidApp
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }
}

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this@AppActivity
//        startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))

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