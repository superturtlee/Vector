package org.matrix.vector.manager.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import org.matrix.vector.manager.R

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    // Smooth fade-in and scale-up animation for the Winged Victory icon
    val alphaAnim by
        animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0f,
            animationSpec = tween(durationMillis = 800),
            label = "SplashAlpha",
        )
    val scaleAnim by
        animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0.8f,
            animationSpec = tween(durationMillis = 800),
            label = "SplashScale",
        )

    // Trigger the animation and navigation
    LaunchedEffect(key1 = true) {
        startAnimation = true
        // Hold the splash screen for 1.5 seconds to show off the design
        // and to give Graph.init() / DaemonClient time to connect and fetch states.
        delay(1500)
        onSplashFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        // Display your new icon
        // We use the monochrome or foreground drawable depending on how you styled it.
        // If your foreground has multiple colors, change `tint` to Color.Unspecified
        Icon(
            painter = painterResource(id = R.drawable.ic_winged_victory),
            contentDescription = "Vector Splash Logo",
            tint = Color.Unspecified,
            modifier = Modifier.fillMaxSize().scale(scaleAnim).alpha(alphaAnim),
        )
    }
}
