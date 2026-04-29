package org.matrix.vector.manager.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.matrix.vector.manager.ui.navigation.MainRoute
import org.matrix.vector.manager.ui.navigation.VectorNavGraph

// 1. A simple data class to hold our tab info cleanly
private data class TabItem(val route: MainRoute, val icon: ImageVector, val label: String)

@Composable
fun VectorApp() {
    val navController = rememberNavController()
    // Observe the current route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val tabs =
        listOf(
            TabItem(MainRoute.Repo, Icons.Rounded.Storefront, "Repo"),
            TabItem(MainRoute.Modules, Icons.Rounded.Extension, "Modules"),
            TabItem(MainRoute.Home, Icons.Rounded.Home, "Home"),
            TabItem(MainRoute.Logs, Icons.Rounded.ReceiptLong, "Logs"),
        )

    // Instead of using Scaffold's default bottomBar, we use a Box.
    // This allows the lists (Modules, Repo) to scroll completely underneath the floating bar!
    Box(modifier = Modifier.fillMaxSize()) {

        // The main content area
        VectorNavGraph(navController = navController, innerPadding = PaddingValues(0.dp))

        if (currentRoute != "Splash") {
            // The Floating Bottom Bar pinned to the bottom center
            FloatingBottomBar(
                tabs = tabs,
                navController = navController,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun FloatingBottomBar(
    tabs: List<TabItem>,
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Get the system navigation bar height so our floating bar doesn't hide behind the Android
    // gesture pill
    val systemNavBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        modifier =
            modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = systemNavBarPadding + 16.dp) // Hover above the system edge
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = MaterialTheme.colorScheme.primary,
                ),
        shape = CircleShape,
        // Make the background 90% opaque so content blurs/shows through slightly
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.30f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val isSelected = currentRoute == tab.route.name

                FancyBottomNavItem(
                    item = tab,
                    isSelected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navController.navigate(tab.route.name) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FancyBottomNavItem(item: TabItem, isSelected: Boolean, onClick: () -> Unit) {
    // Smoothly animate the background color when selected
    val backgroundColor by
        animateColorAsState(
            targetValue =
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            label = "bg_color_anim",
        )

    // Smoothly animate the icon/text color when selected
    val contentColor by
        animateColorAsState(
            targetValue =
                if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            label = "content_color_anim",
        )

    Row(
        modifier =
            Modifier.clip(
                    CircleShape
                ) // 1. Clip the bounds so the ripple and background are rounded
                .background(backgroundColor) // 2. Apply the animated background color
                .clickable(onClick = onClick) // 3. Make it clickable
                .padding(horizontal = 16.dp, vertical = 12.dp) // 4. Add inner padding
                .animateContentSize( // 5. MAGICAL LINE: Animates the width change when text
                    // appears/disappears!
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        )
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )

        // Only show the text label if this tab is currently selected
        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.label,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
