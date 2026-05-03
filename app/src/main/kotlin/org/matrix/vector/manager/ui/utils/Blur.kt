
//copyed from Kernelsu
package org.matrix.vector.manager.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.layerBackdrop

private var globalBackdropInstance: LayerBackdrop? = null
private var isGlobalBackdropInitialized = false

@Composable
fun getGlobalBackdropInstance(): LayerBackdrop? {
    if (!isGlobalBackdropInitialized) {
        globalBackdropInstance = rememberBlurBackdrop()
        isGlobalBackdropInitialized = true
    }
    return globalBackdropInstance
}

@Composable
fun rememberBlurBackdrop(): LayerBackdrop? {
    if (!isRenderEffectSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = if (backdrop != null) {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = 25f * LocalDensity.current.density,
                colors = BlurColors(
                    blendColors = listOf(
                        BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.87f)),
                    ),
                ),
            )
        } else {
            Modifier
        },
    ) {
        content()
    }
}
//Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) 
@Composable
fun CaptureBluredContent(
    backdrop: LayerBackdrop?,
    content: @Composable () -> Unit
) {
    Box(
        modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier
    ) {
        content()
    }
}
//