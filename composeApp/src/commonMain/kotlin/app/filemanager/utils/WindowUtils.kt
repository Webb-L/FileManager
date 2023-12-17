package app.filemanager.utils

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowSizeClass { Compact, Medium, Expanded }


fun calculateWindowSizeClass(maxWidth: Dp, maxHeight: Dp): WindowSizeClass = when {
    maxWidth < 600.dp -> WindowSizeClass.Compact
    maxWidth >= 600.dp && maxWidth < 840.dp -> WindowSizeClass.Medium
    else -> WindowSizeClass.Expanded
}