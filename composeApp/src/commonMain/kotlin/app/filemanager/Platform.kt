package app.filemanager

import app.filemanager.data.main.DeviceType
import app.filemanager.service.data.SocketDevice
import com.russhwolf.settings.Settings

expect fun createSettings(): Settings

expect val PlatformType: DeviceType

expect fun getSocketDevice() : SocketDevice