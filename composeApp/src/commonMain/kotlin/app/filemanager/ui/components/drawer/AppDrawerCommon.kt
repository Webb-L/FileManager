package app.filemanager.ui.components.drawer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppDrawerItem(
    title: String,
    actions: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AppDrawerHeader(title, actions)
    content()
    Spacer(Modifier.height(12.dp))
}

@Composable
fun AppDrawerHeader(title: String, actions: @Composable () -> Unit) {
    Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp)) {
        Text(title)
        Spacer(Modifier.weight(1f))
        actions()
    }
}