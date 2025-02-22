package app.filemanager.data.file

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class FileProtocol(type: String) {
    Local("Local"),
    Device("Device"),
    Network("Network")
}


@Composable
fun FileProtocol.toIcon() {
    when (this) {
        FileProtocol.Local -> {}
        FileProtocol.Device -> {
            Icon(
                Icons.Default.Devices,
                null,
                Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(4.dp))
        }

        FileProtocol.Network -> {
            Icon(Icons.Default.Public, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
        }
    }
}