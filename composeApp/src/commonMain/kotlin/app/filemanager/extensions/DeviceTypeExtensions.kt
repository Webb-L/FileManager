package app.filemanager.extensions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.filemanager.data.main.DeviceType

/**
 * 为设备类型返回对应的图标
 */
fun DeviceType.getIcon(): ImageVector {
    return when (this) {
        DeviceType.Android -> Icons.Default.PhoneAndroid
        DeviceType.IOS -> Icons.Default.PhoneIphone
        DeviceType.JVM -> Icons.Default.Devices
        DeviceType.JS -> Icons.Default.Javascript
    }
}

/**
 * 为设备类型显示对应的图标组件
 * @param contentDescription 图标的内容描述，可选
 * @param modifier 修饰符，可选
 */
@Composable
fun DeviceType.DeviceIcon(
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = this.getIcon(),
        contentDescription = contentDescription ?: when (this) {
            DeviceType.Android -> "Android设备"
            DeviceType.IOS -> "iOS设备"
            DeviceType.JVM -> "PC设备"
            DeviceType.JS -> "浏览器设备"
        },
        modifier = modifier
    )
}