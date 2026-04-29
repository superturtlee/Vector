package org.matrix.vector.manager.ui.screens.modules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import org.matrix.vector.manager.Graph
import org.matrix.vector.manager.data.model.AppInfo

// Factory to inject dependencies AND the specific packageName into the ViewModel
class ScopeViewModelFactory(private val packageName: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ScopeViewModel(
            modulePackageName = packageName,
            daemonClient = Graph.daemonClient,
            appRepository = Graph.appRepository,
        )
            as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScopeScreen(packageName: String, onNavigateBack: () -> Unit) {
    val viewModel: ScopeViewModel = viewModel(factory = ScopeViewModelFactory(packageName))

    // Observe state from ViewModel
    val filteredApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showSystemApps by viewModel.showSystemApps.collectAsState()
    val showGames by viewModel.showGames.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Scope Configuration", style = MaterialTheme.typography.titleMedium)
                        Text(
                            packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // 1. Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search apps...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )

            // 2. Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = showSystemApps,
                    onClick = { viewModel.showSystemApps.value = !showSystemApps },
                    label = { Text("System Apps") },
                )
                FilterChip(
                    selected = showGames,
                    onClick = { viewModel.showGames.value = !showGames },
                    label = { Text("Games") },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 3. App List
            if (filteredApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No apps found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                ) {
                    items(filteredApps, key = { "${it.packageName}_${it.userId}" }) { app ->
                        AppScopeListItem(
                            appInfo = app,
                            onToggle = { isChecked -> viewModel.toggleAppInScope(app, isChecked) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppScopeListItem(appInfo: AppInfo, onToggle: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onToggle(!appInfo.isSelectedInScope) },
        headlineContent = { Text(appInfo.appName, fontWeight = FontWeight.Bold) },
        supportingContent = {
            Text(appInfo.packageName, style = MaterialTheme.typography.bodySmall)
        },
        trailingContent = {
            Checkbox(checked = appInfo.isSelectedInScope, onCheckedChange = { onToggle(it) })
        },
        colors =
            ListItemDefaults.colors(
                containerColor =
                    if (appInfo.isSelectedInScope)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surface
            ),
    )
}
