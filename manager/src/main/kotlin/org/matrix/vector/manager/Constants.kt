package org.matrix.vector.manager

import android.os.IBinder
import kotlin.system.exitProcess
import org.lsposed.lspd.ILSPManagerService

object Constants {
    const val TAG = "VectorManager"

    /** Called via reflection by the Vector daemon to inject the service binder. */
    @JvmStatic
    fun setBinder(binder: IBinder): Boolean {
        val service = ILSPManagerService.Stub.asInterface(binder)

        // Inject the service into our Dependency Graph
        Graph.service = service

        try {
            // Kill the manager process if the daemon crashes/dies
            binder.linkToDeath({ exitProcess(0) }, 0)
        } catch (e: Exception) {
            exitProcess(0)
        }

        return binder.isBinderAlive
    }
}
