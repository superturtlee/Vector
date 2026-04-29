package org.matrix.vector.manager.data.model

import android.content.pm.ApplicationInfo

/** Represents an installed application for the Scope configuration screen. */
data class AppInfo(
    val packageName: String,
    val userId: Int,
    val appName: String,
    val isSystemApp: Boolean,
    val isGame: Boolean,
    val isSelectedInScope: Boolean,
    val isRecommended: Boolean,
    val applicationInfo: ApplicationInfo,
)
