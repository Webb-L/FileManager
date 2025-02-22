import androidx.compose.ui.window.ComposeUIViewController
import app.filemanager.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
