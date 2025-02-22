package app.filemanager.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.filemanager.exception.AuthorityException
import app.filemanager.exception.EmptyDataException
import app.filemanager.utils.WindowSizeClass
import app.filemanager.utils.calculateWindowSizeClass

@Composable
fun GridList(
    isLoading: Boolean = false,
    exception: Throwable? = null,
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit
) {
    if (isLoading) {
        LoadingBase()
        return
    }
    if (!isLoading && exception != null) {
        when (exception) {
            is EmptyDataException -> ErrorEmptyData()
            is AuthorityException -> ErrorBlock(exception.message)
            else -> ErrorBase("出现异常：\n${exception.message}")
        }
        return
    }

    BoxWithConstraints(modifier) {
        val columnCount = when (calculateWindowSizeClass(maxWidth, maxHeight)) {
            WindowSizeClass.Compact -> 1
            WindowSizeClass.Medium -> 2
            WindowSizeClass.Expanded -> 3
        }
        LazyVerticalGrid(columns = GridCells.Fixed(columnCount), content = content)
    }
}