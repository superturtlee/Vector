/*
 * This file is part of Vector.
 *
 * Vector is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Vector is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Vector.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2026 Vector Contributors
 */

package org.matrix.vector.manager.ui.screen

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.matrix.vector.manager.ConfigManager
import org.matrix.vector.manager.R
import org.matrix.vector.manager.util.AppHelper
import org.matrix.vector.manager.util.ApplicationWithEquals
import org.matrix.vector.manager.util.ModuleUtil
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import org.matrix.vector.manager.ui.utils.rememberBlurBackdrop
import org.matrix.vector.manager.ui.utils.BlurredBar
import org.matrix.vector.manager.ui.utils.CaptureBluredContent

@Serializable
data class AppListScreen(
    val packageName: String,
    val userId: Int,
    val fromSelectedUserId: Int = 0
) : AbstractScreen() {

    @Composable
    override fun Display(
        padding: PaddingValues,
        onNavigate: (AbstractScreen) -> Unit,
        onBack: () -> Unit
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val moduleUtil = remember { ModuleUtil.getInstance() }
        val pm = remember { context.packageManager }
        val scrollBehavior = MiuixScrollBehavior()

        var apps by remember { mutableStateOf<List<PackageInfo>>(emptyList()) }
        var scopeStates by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
        var recommendedApps by remember { mutableStateOf<Set<String>>(emptySet()) }
        var isLoading by remember { mutableStateOf(true) }
        var isRefreshing by remember { mutableStateOf(false) }
        var moduleName by remember { mutableStateOf("") }
        var showForceStopDialog by remember { mutableStateOf(false) }
        var showRebootDialog by remember { mutableStateOf(false) }
        var pendingToggle by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
        val pullToRefreshState = rememberPullToRefreshState()

        var backdrop = rememberBlurBackdrop()
        val blurActive = backdrop != null
        val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

        // Handle back press to prevent app exit
        BackHandler(onBack = onBack)

        // 监听 isRefreshing 状态，执行实际的应用列表加载
        LaunchedEffect(isRefreshing) {
            if (isRefreshing) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val module = moduleUtil.getModule(packageName, userId)
                        if (module == null) {
                            withContext(Dispatchers.Main) {
                                isRefreshing = false
                            }
                            return@launch
                        }

                        val name = module.appName
                        val scopeList = module.scopeList ?: emptyList()
                        val recommended = scopeList.toSet()
                        val allApps = AppHelper.getAppList(false)
                        val comparator = AppHelper.getAppListComparator(0, pm)
                        val filteredApps = allApps.filter { app ->
                            AppHelper.shouldShowApp(app, userId, packageName)
                        }.sortedWith(comparator)
                        val scopeListSet = ConfigManager.getModuleScope(packageName)
                        val scopes = scopeListSet.associate {
                            "${it.packageName}_${it.userId}" to true
                        }

                        withContext(Dispatchers.Main) {
                            moduleName = name
                            apps = filteredApps
                            scopeStates = scopes
                            recommendedApps = recommended
                            isRefreshing = false
                        }
                    } catch (e: Exception) {
                        Log.e("AppListScreen", "Failed to refresh apps", e)
                        withContext(Dispatchers.Main) {
                            isRefreshing = false
                        }
                    }
                }
            }
        }

        // 加载应用列表和作用域状态
        LaunchedEffect(packageName, userId) {
            scope.launch(Dispatchers.IO) {
                try {
                    val module = moduleUtil.getModule(packageName, userId)
                    if (module == null) {
                        Log.e("AppListScreen", "Module not found: $packageName for user $userId")
                        withContext(Dispatchers.Main) {
                            onBack()
                        }
                        return@launch
                    }

                    val name = module.appName

                    // 获取推荐作用域列表
                    val scopeList = module.scopeList ?: emptyList()
                    val recommended = scopeList.toSet()

                    // 获取所有应用
                    val allApps = AppHelper.getAppList(false)
                    val comparator = AppHelper.getAppListComparator(0, pm)

                    // 过滤：只显示与模块相同 userId 的应用，并排除特殊应用
                    val filteredApps = allApps.filter { app ->
                        AppHelper.shouldShowApp(app, userId, packageName)
                    }.sortedWith(comparator)

                    // 获取当前作用域
                    val scopeListSet = ConfigManager.getModuleScope(packageName)
                    val scopes = scopeListSet.associate {
                        "${it.packageName}_${it.userId}" to true
                    }

                    withContext(Dispatchers.Main) {
                        moduleName = name
                        apps = filteredApps
                        scopeStates = scopes
                        recommendedApps = recommended
                        isLoading = false
                    }
                } catch (e: Exception) {
                    Log.e("AppListScreen", "Failed to load apps", e)
                    withContext(Dispatchers.Main) {
                        isLoading = false
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                BlurredBar(backdrop){
                    TopAppBar(
                        scrollBehavior = scrollBehavior,
                        color = barColor,
                        title = moduleName,
                        subtitle = packageName,
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = MiuixIcons.Back,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                //直接启动应用 （如果它可以被启动）
                                //在IO线程中获取启动Intent，避免在主线程中进行可能的耗时操作
                                    scope.launch(Dispatchers.IO) {
                                        val launchIntent = pm.getLaunchIntentForPackage(packageName)
                                        if (launchIntent != null) {
                                        context.startActivity(launchIntent)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Settings,
                                    contentDescription = "Settings",
                                    tint = MiuixTheme.colorScheme.onSurface
                                )
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            // 强制停止对话框
            if (showForceStopDialog) {
                OverlayDialog(//这个只能在Sc
                    show = showForceStopDialog,
                    title = stringResource(R.string.force_stop_dlg_title),
                    summary = stringResource(R.string.force_stop_dlg_text),
                    onDismissRequest = {
                        showForceStopDialog = false
                        pendingToggle = null
                    },
                    content = {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                text = stringResource(android.R.string.ok),
                                onClick = {
                                    pendingToggle?.let { (pkgName, _) ->
                                        scope.launch(Dispatchers.IO) {
                                            ConfigManager.forceStopPackage(pkgName, userId)
                                        }
                                    }
                                    showForceStopDialog = false
                                    pendingToggle = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColorsPrimary()
                            )
                            Spacer(Modifier.width(20.dp))
                            TextButton(
                                text = stringResource(android.R.string.cancel),
                                onClick = {
                                    showForceStopDialog = false
                                    pendingToggle = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                )
            }

            // 重启对话框
            if (showRebootDialog) {
                OverlayDialog(
                    show = showRebootDialog,
                    title = stringResource(R.string.reboot),
                    summary = stringResource(R.string.reboot_required),
                    onDismissRequest = { showRebootDialog = false },
                    content = {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                text = stringResource(R.string.reboot),
                                onClick = {
                                    ConfigManager.reboot()
                                    showRebootDialog = false
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColorsPrimary()
                            )
                            Spacer(Modifier.width(20.dp))
                            TextButton(
                                text = stringResource(android.R.string.cancel),
                                onClick = { showRebootDialog = false },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                )
            }

            CaptureBluredContent(backdrop) {
                PullToRefresh(
                    isRefreshing = isRefreshing,
                    pullToRefreshState = pullToRefreshState,
                    onRefresh = { isRefreshing = true },
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        start = 12.dp,
                        end = 12.dp
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding(),
                            start = 12.dp,
                            end = 12.dp
                        )
                    ) {
                        if (isLoading) {
                            item {
                                Card(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.loading),
                                            color = MiuixTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        } else {
                            // 合并显示所有应用，推荐应用排在前面
                            val sortedApps = apps.sortedWith(
                                compareByDescending<PackageInfo> { recommendedApps.contains(it.packageName) }
                                    .thenBy { it.packageName }
                            )

                            items(
                                sortedApps,
                                key = { "${it.packageName}_${it.applicationInfo?.uid ?: 0}" }
                            ) { app ->
                                val appInfo = app.applicationInfo
                                if (appInfo != null) {
                                    val isRecommended = recommendedApps.contains(app.packageName)
                                    AppItem(
                                        app = app,
                                        pm = pm,
                                        userId = userId,
                                        isEnabled = scopeStates["${app.packageName}_${userId}"] ?: false,
                                        isRecommended = isRecommended,
                                        onToggle = { enabled ->
                                            scope.launch(Dispatchers.IO) {
                                                updateScope(
                                                    packageName = packageName,
                                                    appPackageName = app.packageName,
                                                    userId = userId,
                                                    enabled = enabled,
                                                    moduleUtil = moduleUtil,
                                                    onSuccess = { newStates ->
                                                        scopeStates = newStates
                                                    },
                                                    onNeedReboot = {
                                                        showRebootDialog = true
                                                    }
                                                )
                                            }
                                        },
                                        onClick = {
                                            val currentState = scopeStates["${app.packageName}_${userId}"] ?: false
                                            scope.launch(Dispatchers.IO) {
                                                updateScope(
                                                    packageName = packageName,
                                                    appPackageName = app.packageName,
                                                    userId = userId,
                                                    enabled = !currentState,
                                                    moduleUtil = moduleUtil,
                                                    onSuccess = { newStates ->
                                                        scopeStates = newStates
                                                    },
                                                    onNeedReboot = {
                                                        showRebootDialog = true
                                                    }
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            if (app.packageName != "system") {
                                                pendingToggle = app.packageName to scopeStates["${app.packageName}_${userId}"]!!
                                                showForceStopDialog = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateScope(
        packageName: String,
        appPackageName: String,
        userId: Int,
        enabled: Boolean,
        moduleUtil: ModuleUtil,
        onSuccess: (Map<String, Boolean>) -> Unit,
        onNeedReboot: () -> Unit
    ) {
        val key = "${appPackageName}_${userId}"

        // 获取当前所有作用域
        val currentScope = ConfigManager.getModuleScope(packageName).toMutableSet()

        if (enabled) {
            // 添加到作用域
            val newApp = ApplicationWithEquals(appPackageName, userId)
            currentScope.add(newApp)
        } else {
            // 从作用域移除
            currentScope.removeIf { it.packageName == appPackageName && it.userId == userId }
        }

        // 保存作用域
        val module = moduleUtil.getModule(packageName, userId)
        val success = ConfigManager.setModuleScope(packageName, module?.legacy ?: false, currentScope)

        if (success) {
            withContext(Dispatchers.Main) {
                val newStates = ConfigManager.getModuleScope(packageName).associate {
                    "${it.packageName}_${it.userId}" to true
                }
                onSuccess(newStates)

                // 只有 system 包需要提示重启
                if (appPackageName == "system") {
                    onNeedReboot()
                }
            // 其他应用不自动弹窗，用户需要时可以手动重启应用
            }
        }
    }

    @Composable
    fun AppItem(
        app: PackageInfo,
        pm: PackageManager,
        userId: Int,
        isEnabled: Boolean,
        isRecommended: Boolean,
        onToggle: (Boolean) -> Unit,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        var icon by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
        var label by remember { mutableStateOf("") }
        LaunchedEffect(app.packageName) {
            withContext(Dispatchers.IO) {
                try {
                    app.applicationInfo?.let { appInfo ->
                        icon = appInfo.loadIcon(pm).toBitmap()
                    }
                    label = AppHelper.getAppLabel(app, pm)?.toString() ?: app.packageName
                } catch (e: Exception) {
                    label = app.packageName
                }
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            pressFeedbackType = top.yukonga.miuix.kmp.utils.PressFeedbackType.Sink,
            onClick = onClick,
            onLongPress = onLongClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标
                icon?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

            // 应用信息
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MiuixTheme.colorScheme.onSurface
                        )

                        if (isRecommended) {
                            Spacer(modifier = Modifier.width(8.dp))

                            val recommendedBg = MiuixTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                            val recommendedFg = MiuixTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)

                            Box(
                                modifier = Modifier
                                    .background(recommendedBg, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.requested_by_module),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = recommendedFg
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = app.packageName,
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

            // 开关
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }


    override fun isGlobal(): Boolean = true
}

