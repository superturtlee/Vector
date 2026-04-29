package org.matrix.vector.manager.ipc

import android.os.RemoteException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lsposed.lspd.ILSPManagerService

/**
 * Safely wraps synchronous Binder transactions into asynchronous Kotlin Coroutines. Ensures the
 * main UI thread is never blocked by IPC delays or daemon deadlocks.
 */
class DaemonClient(var service: ILSPManagerService? = null) {

    val isAlive: Boolean
        get() = service?.asBinder()?.isBinderAlive == true

    /**
     * Executes a daemon IPC call on the IO thread pool. Wraps the response in a [Result] to handle
     * RemoteExceptions gracefully without crashing.
     */
    private suspend fun <T> runIpc(block: () -> T): Result<T> =
        withContext(Dispatchers.IO) {
            if (!isAlive || service == null) {
                return@withContext Result.failure(IllegalStateException("Daemon is not active"))
            }
            try {
                Result.success(block())
            } catch (e: RemoteException) {
                Result.failure(e)
            }
        }

    // --- Core Methods ---

    suspend fun getXposedApiVersion(): Result<Int> = runIpc { service!!.xposedApiVersion }

    suspend fun getEnabledModules(): Result<List<String>> = runIpc {
        service!!.enabledModules().toList()
    }

    suspend fun setModuleEnabled(packageName: String, enable: Boolean): Result<Boolean> = runIpc {
        if (enable) {
            service!!.enableModule(packageName)
        } else {
            service!!.disableModule(packageName)
        }
    }

    suspend fun getXposedVersionName(): Result<String> = runIpc { service!!.xposedVersionName }

    suspend fun getXposedVersionCode(): Result<Long> = runIpc { service!!.xposedVersionCode }

    suspend fun getInstalledPackagesFromAllUsers(
        flags: Int,
        filterNoProcess: Boolean,
    ): Result<List<android.content.pm.PackageInfo>> = runIpc {
        service!!.getInstalledPackagesFromAllUsers(flags, filterNoProcess).list
    }

    suspend fun setModuleScope(
        packageName: String,
        applications: List<org.lsposed.lspd.models.Application>,
    ): Result<Boolean> = runIpc { service!!.setModuleScope(packageName, applications) }

    suspend fun getModuleScope(
        packageName: String
    ): Result<List<org.lsposed.lspd.models.Application>> = runIpc {
        service!!.getModuleScope(packageName)
    }

    suspend fun enableStatusNotification(): Result<Boolean> = runIpc {
        service!!.enableStatusNotification()
    }

    suspend fun setEnableStatusNotification(enabled: Boolean): Result<Unit> = runIpc {
        service!!.setEnableStatusNotification(enabled)
    }

    suspend fun isVerboseLogEnabled(): Result<Boolean> = runIpc { service!!.isVerboseLog }

    suspend fun setVerboseLogEnabled(enabled: Boolean): Result<Unit> = runIpc {
        service!!.isVerboseLog = enabled
    }

    suspend fun getLog(verbose: Boolean): Result<android.os.ParcelFileDescriptor> = runIpc {
        if (verbose) service!!.verboseLog else service!!.modulesLog
    }

    suspend fun clearLogs(verbose: Boolean): Result<Boolean> = runIpc {
        service!!.clearLogs(verbose)
    }

    suspend fun getPackageInfo(
        packageName: String,
        flags: Int,
        userId: Int,
    ): Result<android.content.pm.PackageInfo> = runIpc {
        service!!.getPackageInfo(packageName, flags, userId)
    }

    suspend fun forceStopPackage(packageName: String, userId: Int): Result<Unit> = runIpc {
        service!!.forceStopPackage(packageName, userId)
    }

    suspend fun reboot(): Result<Unit> = runIpc { service!!.reboot() }

    suspend fun uninstallPackage(packageName: String, userId: Int): Result<Boolean> = runIpc {
        service!!.uninstallPackage(packageName, userId)
    }

    suspend fun isSepolicyLoaded(): Result<Boolean> = runIpc { service!!.isSepolicyLoaded }

    suspend fun getUsers(): Result<List<org.lsposed.lspd.models.UserInfo>> = runIpc {
        service!!.users
    }

    suspend fun installExistingPackageAsUser(packageName: String, userId: Int): Result<Boolean> =
        runIpc {
            val INSTALL_SUCCEEDED = 1
            service!!.installExistingPackageAsUser(packageName, userId) == INSTALL_SUCCEEDED
        }

    suspend fun systemServerRequested(): Result<Boolean> = runIpc {
        service!!.systemServerRequested()
    }

    suspend fun dex2oatFlagsLoaded(): Result<Boolean> = runIpc { service!!.dex2oatFlagsLoaded() }

    suspend fun startActivityAsUserWithFeature(
        intent: android.content.Intent,
        userId: Int,
    ): Result<Int> = runIpc { service!!.startActivityAsUserWithFeature(intent, userId) }

    suspend fun queryIntentActivitiesAsUser(
        intent: android.content.Intent,
        flags: Int,
        userId: Int,
    ): Result<List<android.content.pm.ResolveInfo>> = runIpc {
        service!!.queryIntentActivitiesAsUser(intent, flags, userId).list
    }

    suspend fun setHiddenIcon(hide: Boolean): Result<Unit> = runIpc {
        service!!.setHiddenIcon(hide)
    }

    suspend fun getAutoInclude(packageName: String): Result<Boolean> = runIpc {
        service!!.getAutoInclude(packageName)
    }

    suspend fun setAutoInclude(packageName: String, enable: Boolean): Result<Unit> = runIpc {
        service!!.setAutoInclude(packageName, enable)
    }
}
