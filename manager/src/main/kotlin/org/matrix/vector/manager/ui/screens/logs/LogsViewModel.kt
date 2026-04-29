package org.matrix.vector.manager.ui.screens.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.vector.manager.ipc.DaemonClient

class LogsViewModel(private val daemonClient: DaemonClient) : ViewModel() {

    private val _moduleLogs = MutableStateFlow<List<String>>(emptyList())
    val moduleLogs: StateFlow<List<String>> = _moduleLogs.asStateFlow()

    private val _verboseLogs = MutableStateFlow<List<String>>(emptyList())
    val verboseLogs: StateFlow<List<String>> = _verboseLogs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isVerboseEnabled = MutableStateFlow(false)
    val isVerboseEnabled: StateFlow<Boolean> = _isVerboseEnabled.asStateFlow()

    val wordWrapEnabled = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            _isVerboseEnabled.value = daemonClient.isVerboseLogEnabled().getOrDefault(false)
            refreshLogs()
        }
    }

    fun refreshLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true

            // Load Module Logs
            _moduleLogs.value = readLog(verbose = false)

            // Load Verbose Logs
            _verboseLogs.value = readLog(verbose = true)

            _isLoading.value = false
        }
    }

    private suspend fun readLog(verbose: Boolean): List<String> =
        withContext(Dispatchers.IO) {
            val result = daemonClient.getLog(verbose)
            val pfd =
                result.getOrNull()
                    ?: return@withContext listOf("Failed to load logs or daemon unreachable.")

            return@withContext try {
                FileInputStream(pfd.fileDescriptor).use { fis ->
                    BufferedReader(InputStreamReader(fis)).use { reader ->
                        reader.readLines().ifEmpty { listOf("No logs available.") }
                    }
                }
            } catch (e: Exception) {
                listOf("Error reading log file: ${e.message}")
            } finally {
                pfd.close()
            }
        }

    fun clearLogs(verbose: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = daemonClient.clearLogs(verbose).getOrDefault(false)
            if (success) {
                if (verbose) {
                    _verboseLogs.value = listOf("Verbose logs cleared.")
                } else {
                    _moduleLogs.value = listOf("Module logs cleared.")
                }
            }
        }
    }

    fun toggleVerbose(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            daemonClient.setVerboseLogEnabled(enabled)
            _isVerboseEnabled.value = daemonClient.isVerboseLogEnabled().getOrDefault(false)
        }
    }
}
