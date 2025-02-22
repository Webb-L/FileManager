package app.filemanager.ui.state.main

import androidx.compose.runtime.mutableStateListOf
import app.filemanager.data.main.Network
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkState() {
    private val _isNetworkAdd: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isNetworkAdd: StateFlow<Boolean> = _isNetworkAdd
    fun updateNetworkAdd(value: Boolean) {
        _isNetworkAdd.value = value
    }

    val networks = mutableStateListOf<Network>()
}