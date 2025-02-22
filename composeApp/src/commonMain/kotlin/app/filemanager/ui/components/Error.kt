package app.filemanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// 空数据错误
@Composable
fun ErrorEmptyData() {
    ErrorBase("找不到数据！")
}

// 静止访问错误
@Composable
fun ErrorBlock(message: String?) {
    ErrorBase(message ?: "", Icons.Default.Block)
}

@Composable
fun ErrorBase(text: String, imageVector: ImageVector = Icons.Outlined.Info) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector, null, Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(text)
        }
    }
}