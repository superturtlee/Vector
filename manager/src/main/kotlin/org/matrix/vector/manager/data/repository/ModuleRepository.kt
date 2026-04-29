package org.matrix.vector.manager.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.matrix.vector.manager.ipc.DaemonClient

/**
 * Acts as the Single Source of Truth for all Module-related data. The UI observes [modulesState]
 * and [enabledModulesState] and draws itself automatically.
 */
class ModuleRepository(
    private val daemonClient: DaemonClient,
    private val applicationScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {

    // Internal mutable state. Only this repository can change the data.
    private val _enabledModulesState = MutableStateFlow<Set<String>>(emptySet())
    // Public read-only state for the ViewModels to observe.
    val enabledModulesState: StateFlow<Set<String>> = _enabledModulesState.asStateFlow()

    init {
        // Fetch initial data immediately upon creation
        refreshEnabledModules()
    }

    /** Re-fetches the active modules from the daemon. */
    fun refreshEnabledModules() {
        applicationScope.launch {
            daemonClient
                .getEnabledModules()
                .onSuccess { enabledList -> _enabledModulesState.update { enabledList.toSet() } }
                .onFailure {
                    // Daemon unreachable, fallback to empty or handle error state
                    _enabledModulesState.update { emptySet() }
                }
        }
    }

    /**
     * Requests the daemon to toggle a module's state. If successful, the local state is updated
     * immediately without a full refresh.
     */
    suspend fun toggleModule(packageName: String, enable: Boolean): Boolean {
        val result = daemonClient.setModuleEnabled(packageName, enable)

        if (result.isSuccess && result.getOrNull() == true) {
            _enabledModulesState.update { currentSet ->
                if (enable) currentSet + packageName else currentSet - packageName
            }
            return true
        }
        return false
    }
}
