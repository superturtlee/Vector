package org.matrix.vector.manager.ui.screens.home

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.matrix.vector.manager.Graph
import org.matrix.vector.manager.ipc.DaemonClient

/** Defines all possible states for the Home Screen. */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    data object NotInstalled : HomeUiState

    data class Activated(
        val xposedVersionName: String,
        val xposedVersionCode: Long,
        val androidVersion: String,
        val systemAbi: String,
        val deviceName: String,
    ) : HomeUiState
}

class HomeViewModel(private val daemonClient: DaemonClient) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        checkDaemonStatus()
    }

    fun checkDaemonStatus() {
        viewModelScope.launch {
            _uiState.update { HomeUiState.Loading }

            if (!daemonClient.isAlive) {
                _uiState.update { HomeUiState.NotInstalled }
                return@launch
            }

            // Fetch info in parallel or sequentially. We do sequentially here for simplicity.
            val versionName = daemonClient.getXposedVersionName().getOrDefault("Unknown")
            val versionCode = daemonClient.getXposedVersionCode().getOrDefault(0L)

            val deviceName =
                "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
            val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            val systemAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"

            _uiState.update {
                HomeUiState.Activated(
                    xposedVersionName = versionName,
                    xposedVersionCode = versionCode,
                    androidVersion = androidVersion,
                    systemAbi = systemAbi,
                    deviceName = deviceName,
                )
            }
        }
    }

    // Factory to create the ViewModel with our Graph dependencies
    companion object {
        val Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(Graph.daemonClient) as T
                }
            }
    }
}
