package org.matrix.vector.manager.data.model

import android.content.pm.ApplicationInfo

/** Pure Kotlin data class representing an installed Xposed module. */
data class InstalledModule(
    val packageName: String,
    val userId: Int,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val description: String,
    val minVersion: Int,
    val targetVersion: Int,
    val isLegacy: Boolean,
    val isEnabled: Boolean,
    val applicationInfo: ApplicationInfo, // Kept for Icon loading via Coil/Glide later
)
