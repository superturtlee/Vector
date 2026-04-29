package org.matrix.vector.manager.data.repository

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.matrix.vector.manager.Constants
import org.matrix.vector.manager.data.model.OnlineModule

class RepoRepository(private val okHttpClient: OkHttpClient, private val gson: Gson = Gson()) {
    private val _onlineModules = MutableStateFlow<List<OnlineModule>>(emptyList())
    val onlineModules: StateFlow<List<OnlineModule>> = _onlineModules.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val baseUrls =
        listOf(
            "https://modules.lsposed.org/",
            "https://modules-blogcdn.lsposed.org/",
            "https://modules-cloudflare.lsposed.org/",
        )

    suspend fun refreshModules() =
        withContext(Dispatchers.IO) {
            if (_isRefreshing.value) return@withContext
            _isRefreshing.value = true

            for (baseUrl in baseUrls) {
                val url = baseUrl + "modules.json"
                try {
                    val request = Request.Builder().url(url).build()
                    val response = okHttpClient.newCall(request).execute()

                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: continue
                        val modulesArray =
                            gson.fromJson(bodyString, Array<OnlineModule>::class.java)

                        // Filter out hidden modules
                        _onlineModules.value = modulesArray.filter { it.hide != true }
                        Log.d(
                            Constants.TAG,
                            "Successfully fetched ${_onlineModules.value.size} online modules from $url",
                        )
                        break // Stop trying fallback URLs if successful
                    }
                } catch (e: Exception) {
                    Log.w(Constants.TAG, "Failed to fetch from $url", e)
                }
            }

            _isRefreshing.value = false
        }

    /** Fetches detailed information (including Readme and Releases) for a specific module. */
    suspend fun getModuleDetails(packageName: String): OnlineModule? =
        withContext(Dispatchers.IO) {
            for (baseUrl in baseUrls) {
                try {
                    val url = "${baseUrl}module/$packageName.json"
                    val request = Request.Builder().url(url).build()
                    val response = okHttpClient.newCall(request).execute()

                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: continue
                        return@withContext gson.fromJson(bodyString, OnlineModule::class.java)
                    }
                } catch (e: Exception) {
                    Log.w(Constants.TAG, "Failed to fetch module details from $baseUrl", e)
                }
            }
            null
        }
}
