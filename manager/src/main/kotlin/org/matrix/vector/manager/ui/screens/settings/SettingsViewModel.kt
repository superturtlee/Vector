package org.matrix.vector.manager.ui.screens.settings

import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.matrix.vector.manager.Graph
import org.matrix.vector.manager.data.repository.SettingsRepository
import org.matrix.vector.manager.ipc.DaemonClient

class SettingsViewModel(
    val settingsRepo: SettingsRepository,
    private val daemonClient: DaemonClient,
) : ViewModel() {

    private val _isStatusNotifEnabled = MutableStateFlow(false)
    val isStatusNotifEnabled: StateFlow<Boolean> = _isStatusNotifEnabled.asStateFlow()

    private val _isHiddenIconsEnabled = MutableStateFlow(false)
    val isHiddenIconsEnabled: StateFlow<Boolean> = _isHiddenIconsEnabled.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (daemonClient.isAlive) {
                // Fetch status notification state
                _isStatusNotifEnabled.value =
                    daemonClient.enableStatusNotification().getOrDefault(false)

                // Fetch hidden icons state from global settings
                val hiddenIconSetting =
                    Settings.Global.getInt(
                        Graph.context.contentResolver,
                        "show_hidden_icon_apps_enabled",
                        1,
                    )
                _isHiddenIconsEnabled.value = hiddenIconSetting != 0
            }
        }
    }

    fun setStatusNotification(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = daemonClient.setEnableStatusNotification(enabled).isSuccess
            if (success) _isStatusNotifEnabled.value = enabled
        }
    }

    fun setHiddenIcons(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val success =
                daemonClient
                    .setHiddenIcon(!enabled)
                    .isSuccess // Daemon logic expects "hide" (inverted)
            if (success) _isHiddenIconsEnabled.value = enabled
        }
    }
}
