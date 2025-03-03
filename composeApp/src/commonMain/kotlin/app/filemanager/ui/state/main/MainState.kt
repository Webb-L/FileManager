package app.filemanager.ui.state.main

import app.filemanager.utils.WindowSizeClass
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainState {
    // 是否展开抽屉的状态流
    private val _isExpandDrawer: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isExpandDrawer: StateFlow<Boolean> = _isExpandDrawer

    // 更新是否展开抽屉的方法
    fun updateExpandDrawer(value: Boolean) {
        _isExpandDrawer.value = value
    }

    // 是否为收藏状态的状态流
    private val _isFavorite: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite

    // 更新是否为收藏状态的方法
    fun updateFavorite(value: Boolean) {
        _isFavorite.value = value
    }

    // 是否为编辑路径状态的状态流
    private val _isEditPath: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isEditPath: StateFlow<Boolean> = _isEditPath

    // 更新是否为编辑路径状态的方法
    fun updateEditPath(value: Boolean) {
        _isEditPath.value = value
    }

    private val _screen: MutableStateFlow<Screen?> = MutableStateFlow(null)
    val screen: StateFlow<Screen?> = _screen

    fun updateScreen(value: Screen?) {
        _screen.value = value
    }

    var windowSize: WindowSizeClass = WindowSizeClass.Expanded
}
