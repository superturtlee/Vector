package org.matrix.vector.manager.ui.screens.settings

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.matrix.vector.manager.Graph

class SettingsViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(Graph.settingsRepository, Graph.daemonClient) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory())) {
    val context = LocalContext.current

    // Observe Local Preferences
    val themeMode by viewModel.settingsRepo.themeMode.collectAsState()
    val dynamicColor by viewModel.settingsRepo.dynamicColor.collectAsState()
    val amoledBlack by viewModel.settingsRepo.amoledBlack.collectAsState()
    val updateChannel by viewModel.settingsRepo.updateChannel.collectAsState()
    val dohEnabled by viewModel.settingsRepo.dohEnabled.collectAsState()

    // Observe Daemon Settings
    val statusNotifEnabled by viewModel.isStatusNotifEnabled.collectAsState()
    val hiddenIconsEnabled by viewModel.isHiddenIconsEnabled.collectAsState()

    // Backup & Restore Launchers (Requires BackupUtils implementation to do the actual file
    // read/write)
    val backupLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/gzip")
        ) { uri ->
            if (uri != null)
                Toast.makeText(context, "Backup selected: $uri", Toast.LENGTH_SHORT).show()
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null)
                Toast.makeText(context, "Restore selected: $uri", Toast.LENGTH_SHORT).show()
        }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            // --- DAEMON SETTINGS ---
            item { SettingsHeader("Framework") }

            item {
                SettingsSwitch(
                    title = "Status Notification",
                    subtitle = "Show Vector status in the notification panel",
                    icon = Icons.Rounded.Notifications,
                    checked = statusNotifEnabled,
                    onCheckedChange = { viewModel.setStatusNotification(it) },
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                item {
                    SettingsSwitch(
                        title = "Show Hidden Icons",
                        subtitle = "Enable launching apps with hidden launcher icons",
                        icon = Icons.Rounded.Visibility,
                        checked = hiddenIconsEnabled,
                        onCheckedChange = { viewModel.setHiddenIcons(it) },
                    )
                }
            }

            // --- BACKUP & RESTORE ---
            item { SettingsHeader("Data") }

            item {
                SettingsClickable(
                    title = "Backup",
                    subtitle = "Export modules and scope configuration",
                    icon = Icons.Rounded.Backup,
                    onClick = {
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                        val timestamp = LocalDateTime.now().format(formatter)
                        backupLauncher.launch("Vector_$timestamp.lsp")
                    },
                )
            }
            item {
                SettingsClickable(
                    title = "Restore",
                    subtitle = "Import configuration from a backup file",
                    icon = Icons.Rounded.Restore,
                    onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                )
            }

            // --- UI & THEME ---
            item { SettingsHeader("Personalization") }

            item {
                // In a real app, this would open a DropdownMenu or Dialog to pick
                // Light/Dark/System.
                // For simplicity, we toggle between system and dark.
                SettingsClickable(
                    title = "Theme Mode",
                    subtitle = themeMode.uppercase(),
                    icon = Icons.Rounded.DarkMode,
                    onClick = {
                        val nextMode = if (themeMode == "system") "dark" else "system"
                        viewModel.settingsRepo.setThemeMode(nextMode)
                    },
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    SettingsSwitch(
                        title = "Dynamic Colors (Material You)",
                        subtitle = "Extract colors from your wallpaper",
                        icon = Icons.Rounded.Palette,
                        checked = dynamicColor,
                        onCheckedChange = { viewModel.settingsRepo.setDynamicColor(it) },
                    )
                }
            }

            item {
                SettingsSwitch(
                    title = "AMOLED Black",
                    subtitle = "Use pitch black in dark mode",
                    icon = Icons.Rounded.Contrast,
                    checked = amoledBlack,
                    onCheckedChange = { viewModel.settingsRepo.setAmoledBlack(it) },
                )
            }

            // --- NETWORK ---
            item { SettingsHeader("Network & Updates") }

            item {
                SettingsSwitch(
                    title = "DNS over HTTPS (Cloudflare)",
                    subtitle = "Secure repository traffic via DoH",
                    icon = Icons.Rounded.Security,
                    checked = dohEnabled,
                    onCheckedChange = { viewModel.settingsRepo.setDohEnabled(it) },
                )
            }
        }
    }
}

@Composable
private fun SettingsHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 72.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = null)
        }, // null because the ListItem handles the click
    )
}

@Composable
private fun SettingsClickable(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
    )
}
