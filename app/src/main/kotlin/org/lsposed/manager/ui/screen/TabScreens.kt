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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.lsposed.manager.R
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.All
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme
import org.lsposed.manager.ui.utils.rememberBlurBackdrop
import org.lsposed.manager.ui.utils.BlurredBar
import org.lsposed.manager.ui.utils.CaptureBluredContent
import kotlin.math.abs

@Serializable
data class TabScreens(
    val isBinderAlive: Boolean = true,
    val initialTabIndex: Int = 1
) : AbstractScreen() {

    val modulesScreen = ModulesScreen()
    val homeScreen = HomeScreen()
    val logsScreen = LogsScreen()
    val settingsScreen = SettingsScreen()

    @Composable
    override fun Display(
        padding: PaddingValues,
        onNavigate: (AbstractScreen) -> Unit,
        onBack: () -> Unit
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current

        // 创建模糊背景捕获器（仿照KernelSU的实现）
        val backdrop = rememberBlurBackdrop()

         // 根据是否支持模糊来决定NavigationBar的颜色

        // 根据是否支持模糊来决定NavigationBar的颜色
        val navBarColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface

        // 使用rememberSaveable保存当前选中的tab，横竖屏切换时保持状态
        var currentTabIndex by rememberSaveable { mutableIntStateOf(if (initialTabIndex == 1) 0 else initialTabIndex) }

        val pageCount = if (isBinderAlive) 4 else 3
        val pagerState = rememberPagerState(
            initialPage = currentTabIndex,
            pageCount = { pageCount }
        )

        // 监听页面切换，调用对应screen的Refresh
        LaunchedEffect(pagerState.settledPage) {
            when (pagerState.settledPage) {
                0 -> homeScreen.Refresh()
                1 -> modulesScreen.Refresh()
                2 -> if (isBinderAlive) {
                    logsScreen.Refresh()
                } else {
                    settingsScreen.Refresh()
                }
                3 -> settingsScreen.Refresh()
            }
        }

        BackHandler(enabled = pagerState.settledPage != 0) {
            scope.launch {
                val distance = abs(0 - pagerState.currentPage).coerceAtLeast(1)
                val duration = 100 * distance + 100
                currentTabIndex = 0
                pagerState.animateScrollToPage(
                    page = 0,
                    animationSpec = tween(durationMillis = duration, easing = EaseInOut)
                )
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                // 使用Box包装NavigationBar并应用模糊效果（仿照KernelSU的BlurredBar）
                BlurredBar(backdrop) {
                    NavigationBar(
                        color = navBarColor
                    ) {
                        NavigationBarItem(
                            selected = currentTabIndex == 0,
                            onClick = {
                                scope.launch {
                                    val distance = abs(0 - pagerState.currentPage).coerceAtLeast(1)
                                    val duration = 100 * distance + 100
                                    currentTabIndex = 0
                                    pagerState.animateScrollToPage(
                                        page = 0,
                                        animationSpec = tween(durationMillis = duration, easing = EaseInOut)
                                    )
                                }
                            },
                            icon = MiuixIcons.Album,
                            label = context.getString(R.string.overview)
                        )

                        NavigationBarItem(
                            selected = currentTabIndex == 1,
                            onClick = {
                                scope.launch {
                                    val distance = abs(1 - pagerState.currentPage).coerceAtLeast(1)
                                    val duration = 100 * distance + 100
                                    currentTabIndex = 1
                                    pagerState.animateScrollToPage(
                                        page = 1,
                                        animationSpec = tween(durationMillis = duration, easing = EaseInOut)
                                    )
                                }
                            },
                            icon = MiuixIcons.All,
                            label = context.getString(R.string.Modules)
                        )

                        if (isBinderAlive) {
                            NavigationBarItem(
                                selected = currentTabIndex == 2,
                                onClick = {
                                    scope.launch {
                                        val distance = abs(2 - pagerState.currentPage).coerceAtLeast(1)
                                        val duration = 100 * distance + 100
                                        currentTabIndex = 2
                                        pagerState.animateScrollToPage(
                                            page = 2,
                                            animationSpec = tween(durationMillis = duration, easing = EaseInOut)
                                        )
                                    }
                                },
                                icon = MiuixIcons.File,
                                label = context.getString(R.string.Logs)
                            )
                        }

                        NavigationBarItem(
                            selected = currentTabIndex == (if (isBinderAlive) 3 else 2),
                            onClick = {
                                scope.launch {
                                    val targetPage = if (isBinderAlive) 3 else 2
                                    val distance = abs(targetPage - pagerState.currentPage).coerceAtLeast(1)
                                    val duration = 100 * distance + 100
                                    currentTabIndex = targetPage
                                    pagerState.animateScrollToPage(
                                        page = targetPage,
                                        animationSpec = tween(durationMillis = duration, easing = EaseInOut)
                                    )
                                }
                            },
                            icon = MiuixIcons.Settings,
                            label = context.getString(R.string.Settings)
                        )
                    }
                }
            }
        ) { innerPadding ->
            CaptureBluredContent(backdrop) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 2,
                userScrollEnabled = true,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> homeScreen.Display(innerPadding, onNavigate, onBack)
                    1 -> modulesScreen.Display(innerPadding, onNavigate, onBack)
                    2 -> if (isBinderAlive) {
                        logsScreen.Display(innerPadding, onNavigate, onBack)
                    } else {
                        settingsScreen.Display(innerPadding, onNavigate, onBack)
                    }
                    3 -> settingsScreen.Display(innerPadding, onNavigate, onBack)
                }
            }
            }
        }
    }

    override fun isGlobal(): Boolean = false
}
