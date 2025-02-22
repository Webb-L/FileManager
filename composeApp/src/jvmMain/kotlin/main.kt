
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.filemanager.App
import app.filemanager.service.rpc.startHttpShareFileServer
import app.filemanager.service.rpc.startRpcServer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.awt.Dimension

@OptIn(DelicateCoroutinesApi::class)
fun main() = application {
    Window(
        title = "FileManager",
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(350, 600)
        App()
    }

    GlobalScope.launch(Dispatchers.IO) {
        startRpcServer()
    }
    GlobalScope.launch(Dispatchers.IO) {
        startHttpShareFileServer()
    }
}