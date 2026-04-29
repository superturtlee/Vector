package org.matrix.vector.manager

import android.annotation.SuppressLint
import android.content.Context
import okhttp3.OkHttpClient
import org.lsposed.lspd.ILSPManagerService
import org.matrix.vector.manager.data.repository.AppRepository
import org.matrix.vector.manager.data.repository.ModuleRepository
import org.matrix.vector.manager.data.repository.RepoRepository
import org.matrix.vector.manager.data.repository.SettingsRepository
import org.matrix.vector.manager.ipc.DaemonClient

/**
 * A lightweight Dependency Injection container. This holds our singleton instances, where
 * Constants.setBinder(binder) will initialize the DaemonClient.
 */
@SuppressLint("StaticFieldLeak") // Application context doesn't leak
object Graph {
    lateinit var context: Context
        private set

    val daemonClient = DaemonClient()
    val moduleRepository = ModuleRepository(daemonClient)
    lateinit var appRepository: AppRepository
        private set

    val repoRepository = RepoRepository(OkHttpClient())
    lateinit var settingsRepository: SettingsRepository
        private set

    var service: ILSPManagerService? = null
        set(value) {
            field = value
            daemonClient.service = value
            // If the daemon connects, immediately tell the repository to fetch modules
            if (value != null) {
                moduleRepository.refreshEnabledModules()
            }
        }

    fun init(applicationContext: Context) {
        context = applicationContext
        settingsRepository = SettingsRepository(context)
        appRepository = AppRepository(daemonClient, context.packageManager)
    }
}
