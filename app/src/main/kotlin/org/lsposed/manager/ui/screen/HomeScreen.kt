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

package org.lsposed.manager.ui.screen

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.lsposed.manager.App
import org.lsposed.manager.BuildConfig
import org.lsposed.manager.ConfigManager
import org.lsposed.manager.R
import org.lsposed.manager.util.ModuleUtil
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Serializable
data class HomeScreen(val dummy: Int = 0) : AbstractScreen() {

    private var refreshCallback: (() -> Unit)? = null

    override fun Refresh() {
        refreshCallback?.invoke()
    }

    @Composable
    override fun Display(
        padding: PaddingValues,
        onNavigate: (AbstractScreen) -> Unit,
        onBack: () -> Unit
    ) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val scope = rememberCoroutineScope()
    val moduleUtil = remember { ModuleUtil.getInstance() }

    var binderAlive by remember { mutableStateOf(false) }
    var statusTitle by remember { mutableStateOf("") }
    var statusSummary by remember { mutableStateOf("") }
    var apiVersion by remember { mutableStateOf("") }
    var frameworkVersion by remember { mutableStateOf("") }
    var systemVersion by remember { mutableStateOf("") }
    var device by remember { mutableStateOf("") }
    var systemAbi by remember { mutableStateOf("") }
    var enabledModulesCount by remember { mutableStateOf(-1) }

    // 设置刷新回调
    DisposableEffect(Unit) {
        refreshCallback = {
            enabledModulesCount = moduleUtil.enabledModulesCount
        }
        onDispose {
            refreshCallback = null
        }
    }

    // 监听模块加载完成事件，直接更新数据
    DisposableEffect(Unit) {
        val listener = object : ModuleUtil.ModuleListener {
            override fun onModulesReloaded() {
                enabledModulesCount = moduleUtil.enabledModulesCount
            }
        }
        moduleUtil.addListener(listener)
        onDispose {
            moduleUtil.removeListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        binderAlive = ConfigManager.isBinderAlive()

        if (binderAlive) {
            statusTitle = context.getString(R.string.activated)
            statusSummary = String.format(
                "%s (%d)",
                ConfigManager.getXposedVersionName(),
                ConfigManager.getXposedVersionCode()
            )

            apiVersion = ConfigManager.getXposedApiVersion().toString()
            frameworkVersion = String.format(
                "%s (%d)",
                ConfigManager.getXposedVersionName(),
                ConfigManager.getXposedVersionCode()
            )
        } else {
            statusTitle = context.getString(R.string.not_installed)
            statusSummary = context.getString(R.string.not_install_summary)
            apiVersion = context.getString(R.string.not_installed)
            frameworkVersion = context.getString(R.string.not_installed)
        }

        systemVersion = if (Build.VERSION.PREVIEW_SDK_INT != 0) {
            String.format(
                "%s Preview (API %d)",
                Build.VERSION.CODENAME,
                Build.VERSION.SDK_INT
            )
        } else {
            String.format(
                "%s (API %d)",
                Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT
            )
        }

        device = getDeviceInfo()
        systemAbi = Build.SUPPORTED_ABIS[0]
    }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.app_name),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        onClick = { showMoreMenu = true }
                    ) {
                        Icon(
                            imageVector = MiuixIcons.More,
                            contentDescription = "More"
                        )
                    }

                    OverlayListPopup(
                        show = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        alignment = PopupPositionProvider.Align.BottomEnd
                    ) {
                        ListPopupColumn {
                            DropdownImpl(
                                text = stringResource(R.string.feedback_or_suggestion),
                                optionSize = 2,
                                isSelected = false,
                                index = 0,
                                onSelectedIndexChange = { index ->
                                    showMoreMenu = false
                                    if (index == 0) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/JingMatrix/LSPosed/issues/new/choose"))
                                        context.startActivity(intent)
                                    }
                                }
                            )
                            DropdownImpl(
                                text = stringResource(R.string.About),
                                optionSize = 2,
                                isSelected = false,
                                index = 1,
                                onSelectedIndexChange = { index ->
                                    showMoreMenu = false
                                    if (index == 1) {
                                        showAboutDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding()
            )
        ) {
            item {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Status Card - Two column layout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left: Status Card (Activated/Not Activated)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            colors = CardDefaults.defaultColors(
                                color = if (binderAlive) {
                                    if (isSystemInDarkTheme()) {
                                        Color(0xFF1A3825)
                                    } else {
                                        Color(0xFFDFFAE4)
                                    }
                                } else {
                                    MiuixTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .offset(38.dp, 45.dp),
                                    contentAlignment = Alignment.BottomEnd
                                ) {
                                    Icon(
                                        modifier = Modifier.size(170.dp),
                                        imageVector = if (binderAlive) {
                                            Icons.Rounded.CheckCircleOutline
                                        } else {
                                            Icons.Rounded.ErrorOutline
                                        },
                                        tint = if (binderAlive) {
                                            Color(0xFF36D167)
                                        } else {
                                            MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.3f)
                                        },
                                        contentDescription = null
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(all = 16.dp)
                                ) {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = statusTitle,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = statusSummary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Right: Two small cards (Enabled Modules & API Version)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                insideMargin = PaddingValues(16.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = stringResource(R.string.enabled),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    )
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = if (enabledModulesCount >= 0) enabledModulesCount.toString() else "-",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MiuixTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                insideMargin = PaddingValues(16.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = stringResource(R.string.info_api_version),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    )
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = apiVersion,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MiuixTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    // Info Card
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoItem(stringResource(R.string.info_framework_version), frameworkVersion)
                            InfoItem(stringResource(R.string.info_system_version), systemVersion)
                            InfoItem(stringResource(R.string.info_device), device)
                            InfoItem(stringResource(R.string.info_system_abi), systemAbi, bottomPadding = 0.dp)
                        }
                    }
                }
            }
        }
    }

    // About Dialog
    OverlayDialog(
        show = showAboutDialog,
        title = stringResource(R.string.app_name),
        summary = stringResource(
            R.string.about_view_source_code,
            "GitHub: https://github.com/JingMatrix/LSPosed",
            "Telegram: https://t.me/LSPosed"
        ) + "\n\n" + stringResource(R.string.app_name) + " " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")",
        onDismissRequest = { showAboutDialog = false }
    ) {
        Button(
            onClick = { showAboutDialog = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(android.R.string.ok))
        }
    }
    }

    override fun isGlobal(): Boolean = false
}

@Composable
private fun InfoItem(label: String, value: String, bottomPadding: androidx.compose.ui.unit.Dp = 24.dp) {
    Text(
        text = label,
        fontSize = MiuixTheme.textStyles.headline1.fontSize,
        fontWeight = FontWeight.Medium,
        color = MiuixTheme.colorScheme.onSurface
    )
    Text(
        text = value,
        fontSize = MiuixTheme.textStyles.body2.fontSize,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(top = 2.dp, bottom = bottomPadding)
    )
}

private fun getDeviceInfo(): String {
    var manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    if (Build.BRAND != Build.MANUFACTURER) {
        manufacturer += " " + Build.BRAND.replaceFirstChar { it.uppercase() }
    }
    manufacturer += " " + Build.MODEL
    return manufacturer
}
