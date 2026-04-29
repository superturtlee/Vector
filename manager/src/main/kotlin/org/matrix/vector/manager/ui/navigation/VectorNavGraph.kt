package org.matrix.vector.manager.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.matrix.vector.manager.ui.screens.home.HomeScreen
import org.matrix.vector.manager.ui.screens.logs.LogsScreen
import org.matrix.vector.manager.ui.screens.modules.ModulesScreen
import org.matrix.vector.manager.ui.screens.modules.ScopeScreen
import org.matrix.vector.manager.ui.screens.repo.RepoDetailsScreen
import org.matrix.vector.manager.ui.screens.repo.RepoScreen
import org.matrix.vector.manager.ui.screens.settings.SettingsScreen
import org.matrix.vector.manager.ui.screens.splash.SplashScreen

/** Type-safe routing for the bottom navigation tabs. */
enum class MainRoute(val title: String) {
    Repo("Repo"),
    Modules("Modules"),
    Home("Home"),
    Logs("Logs"),
    Settings("Settings"),
}

@Composable
fun VectorNavGraph(navController: NavHostController, innerPadding: PaddingValues) {
    // NavHost manages the swapping of screens
    NavHost(
        navController = navController,
        startDestination = "Splash",
        modifier = Modifier.padding(innerPadding),
    ) {
        composable("Splash") {
            SplashScreen(
                onSplashFinished = {
                    // Navigate to Home and remove Splash from the backstack
                    // so the user can't press "Back" to return to the splash screen.
                    navController.navigate(MainRoute.Home.name) {
                        popUpTo("Splash") { inclusive = true }
                    }
                }
            )
        }

        composable(MainRoute.Repo.name) {
            RepoScreen(
                onModuleClick = { packageName ->
                    // Navigation to the Repo Details page (Readme, Releases) which we will build
                    // later
                    navController.navigate("RepoDetails/$packageName")
                }
            )
        }
        composable("RepoDetails/{packageName}") { backStackEntry ->
            val pkg = backStackEntry.arguments?.getString("packageName") ?: return@composable
            RepoDetailsScreen(packageName = pkg, onNavigateBack = { navController.popBackStack() })
        }
        composable(MainRoute.Modules.name) {
            ModulesScreen(
                onModuleClick = { packageName, userId ->
                    navController.navigate("Scope/$packageName/$userId")
                }
            )
        }
        composable("Scope/{packageName}/{userId}") { backStackEntry ->
            val pkg = backStackEntry.arguments?.getString("packageName") ?: return@composable
            val uid = backStackEntry.arguments?.getString("userId")?.toIntOrNull() ?: 0

            ScopeScreen(packageName = pkg, onNavigateBack = { navController.popBackStack() })
        }
        composable(MainRoute.Home.name) {
            HomeScreen(onNavigateToSettings = { navController.navigate(MainRoute.Settings.name) })
        }
        composable(MainRoute.Logs.name) { LogsScreen() }
        composable(MainRoute.Settings.name) { SettingsScreen() }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title)
    }
}
