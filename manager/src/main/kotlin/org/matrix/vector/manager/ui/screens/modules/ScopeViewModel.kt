package org.matrix.vector.manager.ui.screens.modules

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.lsposed.lspd.models.Application
import org.matrix.vector.manager.Constants
import org.matrix.vector.manager.data.model.AppInfo
import org.matrix.vector.manager.data.repository.AppRepository
import org.matrix.vector.manager.ipc.DaemonClient

/**
 * A lightweight Kotlin data class to guarantee proper equals() and hashCode() functionality when
 * adding/removing elements from the Set.
 */
private data class ScopeTarget(val packageName: String, val userId: Int)

class ScopeViewModel(
    private val modulePackageName: String,
    private val daemonClient: DaemonClient,
    private val appRepository: AppRepository,
) : ViewModel() {

    // 1. Raw Data States
    private val allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val currentScope = MutableStateFlow<Set<ScopeTarget>>(emptySet())

    // 2. Filter States
    val searchQuery = MutableStateFlow("")
    val showSystemApps = MutableStateFlow(false)
    val showGames = MutableStateFlow(true)

    // 3. The Combined Reactive UI State
    // This automatically recalculates whenever ANY of the above states change.
    val filteredApps: StateFlow<List<AppInfo>> =
        combine(allApps, currentScope, searchQuery, showSystemApps, showGames) {
                apps,
                scope,
                query,
                showSys,
                showGame ->
                apps
                    .filter { app ->
                        val matchesQuery =
                            app.appName.contains(query, ignoreCase = true) ||
                                app.packageName.contains(query, ignoreCase = true)
                        val matchesSys = showSys || !app.isSystemApp
                        val matchesGame = showGame || !app.isGame

                        matchesQuery && matchesSys && matchesGame
                    }
                    .map { app ->
                        // Map the selection state
                        val isSelected =
                            scope.any {
                                it.packageName == app.packageName && it.userId == app.userId
                            }
                        app.copy(isSelectedInScope = isSelected)
                    }
                    .sortedBy { it.appName.lowercase() }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            // Fetch apps
            val apps = appRepository.getInstalledApps()
            allApps.value = apps

            // Fetch current scope from daemon
            val scopeResult = daemonClient.getModuleScope(modulePackageName)
            if (scopeResult.isSuccess) {
                // Map AIDL models to our data class for safe Set operations
                val scopeList = scopeResult.getOrNull() ?: emptyList()
                currentScope.value =
                    scopeList.map { ScopeTarget(it.packageName, it.userId) }.toSet()
                Log.d(
                    Constants.TAG,
                    "Loaded ${currentScope.value.size} scope targets for $modulePackageName",
                )
            } else {
                Log.e(Constants.TAG, "Failed to load module scope for $modulePackageName")
            }
        }
    }

    /** Called by the UI when a user clicks a checkbox. */
    fun toggleAppInScope(appInfo: AppInfo, isChecked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val target = ScopeTarget(appInfo.packageName, appInfo.userId)

            val newScope =
                if (isChecked) {
                    currentScope.value + target
                } else {
                    currentScope.value - target
                }

            Log.d(
                Constants.TAG,
                "Toggling ${appInfo.packageName} to $isChecked. New scope size: ${newScope.size}",
            )

            // Convert back to AIDL models for IPC communication
            val aidlList =
                newScope.map { scopeTarget ->
                    Application().apply {
                        packageName = scopeTarget.packageName
                        userId = scopeTarget.userId
                    }
                }

            // NOTE: In the future, if this module is a "Legacy" module, we must auto-inject
            // the module's own package name into this AIDL list before saving.
            // Tell the daemon to save the new scope
            val success =
                daemonClient.setModuleScope(modulePackageName, aidlList).getOrDefault(false)

            if (success) {
                currentScope.value = newScope
                Log.d(Constants.TAG, "Scope successfully saved to daemon.")
            } else {
                Log.e(Constants.TAG, "Daemon rejected scope update.")
            }
        }
    }
}
