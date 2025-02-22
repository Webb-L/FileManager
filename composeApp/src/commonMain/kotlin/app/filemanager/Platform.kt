package app.filemanager

import app.filemanager.data.main.DeviceType
import com.russhwolf.settings.Settings

expect fun createSettings(): Settings

expect val PlatformType: DeviceType