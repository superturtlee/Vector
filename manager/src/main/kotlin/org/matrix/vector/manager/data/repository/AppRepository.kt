package org.matrix.vector.manager.data.repository

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matrix.vector.manager.data.model.AppInfo
import org.matrix.vector.manager.ipc.DaemonClient

/** Fetches and caches the list of installed applications from the daemon. */
class AppRepository(
    private val daemonClient: DaemonClient,
    private val packageManager: PackageManager,
) {
    private var cachedApps: List<AppInfo>? = null

    suspend fun getInstalledApps(forceRefresh: Boolean = false): List<AppInfo> =
        withContext(Dispatchers.IO) {
            if (!forceRefresh && cachedApps != null) {
                return@withContext cachedApps!!
            }

            // MATCH_UNINSTALLED_PACKAGES | GET_META_DATA
            val flags = PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.GET_META_DATA

            val result =
                daemonClient.getInstalledPackagesFromAllUsers(flags, filterNoProcess = true)
            if (result.isFailure) return@withContext emptyList()

            val packages = result.getOrNull() ?: emptyList()
            val PER_USER_RANGE = 100000

            val appList =
                packages.mapNotNull { pkg ->
                    val appInfo = pkg.applicationInfo ?: return@mapNotNull null
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isGame =
                        appInfo.category == ApplicationInfo.CATEGORY_GAME ||
                            (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0

                    val userId = appInfo.uid / PER_USER_RANGE

                    AppInfo(
                        packageName = pkg.packageName,
                        userId = userId,
                        appName = appInfo.loadLabel(packageManager).toString(),
                        isSystemApp = isSystem,
                        isGame = isGame,
                        isSelectedInScope = false, // To be merged later in the ViewModel
                        isRecommended = false,
                        applicationInfo = appInfo,
                    )
                }

            cachedApps = appList
            return@withContext appList
        }
}
