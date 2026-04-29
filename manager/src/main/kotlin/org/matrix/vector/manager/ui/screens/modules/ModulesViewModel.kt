package org.matrix.vector.manager.ui.screens.modules

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lsposed.lspd.models.UserInfo
import org.matrix.vector.manager.data.model.InstalledModule
import org.matrix.vector.manager.ipc.DaemonClient

/** Represents the data for a single tab (A single user and their modules). */
data class UserModulesState(val user: UserInfo, val modules: List<InstalledModule>)

class ModulesViewModel(
    private val daemonClient: DaemonClient,
    private val packageManager: PackageManager,
) : ViewModel() {

    // The state exposed to the UI: A list of tabs, where each tab contains the user and their
    // modules.
    private val _userModulesTabs = MutableStateFlow<List<UserModulesState>>(emptyList())
    val userModulesTabs: StateFlow<List<UserModulesState>> = _userModulesTabs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadModules()
    }

    fun loadModules() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.update { true }

            // 1. Fetch Users
            val usersResult = daemonClient.getUsers()
            val users = usersResult.getOrNull() ?: emptyList()

            // 2. Fetch Packages
            val flags =
                PackageManager.GET_META_DATA or
                    PackageManager.MATCH_UNINSTALLED_PACKAGES or
                    0x00400000 // MATCH_ANY_USER

            val pkgsResult =
                daemonClient.getInstalledPackagesFromAllUsers(flags, filterNoProcess = false)
            val packages = pkgsResult.getOrNull() ?: emptyList()

            // 3. Filter for Xposed Modules and map to our data class
            val PER_USER_RANGE = 100000
            val allModules =
                packages.mapNotNull { pkg ->
                    val appInfo = pkg.applicationInfo ?: return@mapNotNull null

                    // Logic adapted from ModuleUtil.isLegacyModule / getModernModuleApk
                    // For simplicity here, we check meta-data.
                    // (In a full implementation, we'd also check the zip file for java_init.list)
                    val isLegacy = appInfo.metaData?.containsKey("xposedminversion") == true
                    val hasXposedMeta = appInfo.metaData?.containsKey("xposedmodule") == true

                    if (!isLegacy && !hasXposedMeta) return@mapNotNull null

                    val userId = appInfo.uid / PER_USER_RANGE

                    InstalledModule(
                        packageName = pkg.packageName,
                        userId = userId,
                        appName = appInfo.loadLabel(packageManager).toString(),
                        versionName = pkg.versionName ?: "",
                        versionCode = pkg.longVersionCode,
                        description = appInfo.loadDescription(packageManager)?.toString() ?: "",
                        minVersion = appInfo.metaData?.getInt("xposedminversion", 0) ?: 0,
                        targetVersion = 0, // Extract from modern module prop if needed
                        isLegacy = isLegacy,
                        isEnabled = false, // Will be combined with ModuleRepository state later
                        applicationInfo = appInfo,
                    )
                }

            // 4. Group modules by User
            val tabs =
                users.map { user ->
                    UserModulesState(
                        user = user,
                        // Sort modules alphabetically
                        modules =
                            allModules
                                .filter { it.userId == user.id }
                                .sortedBy { it.appName.lowercase() },
                    )
                }

            _userModulesTabs.update { tabs }
            _isLoading.update { false }
        }
    }
}
