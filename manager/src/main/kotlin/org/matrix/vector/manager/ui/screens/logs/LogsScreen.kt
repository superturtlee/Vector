package org.matrix.vector.manager.ui.screens.logs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WrapText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.matrix.vector.manager.Graph

class LogsViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LogsViewModel(Graph.daemonClient) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LogsScreen(viewModel: LogsViewModel = viewModel(factory = LogsViewModelFactory())) {
    val moduleLogs by viewModel.moduleLogs.collectAsState()
    val verboseLogs by viewModel.verboseLogs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isVerboseEnabled by viewModel.isVerboseEnabled.collectAsState()
    val wordWrapEnabled by viewModel.wordWrapEnabled.collectAsState()

    val tabs = listOf("Module", "Verbose")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logs") },
                actions = {
                    // Toggle Word Wrap
                    IconButton(onClick = { viewModel.wordWrapEnabled.value = !wordWrapEnabled }) {
                        Icon(
                            imageVector = Icons.Rounded.WrapText,
                            contentDescription = "Word Wrap",
                            tint =
                                if (wordWrapEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Refresh Logs
                    IconButton(onClick = { viewModel.refreshLogs() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Logs")
                    }
                    // Clear Logs
                    IconButton(
                        onClick = { viewModel.clearLogs(verbose = pagerState.currentPage == 1) }
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Clear Logs",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // Tab Row
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(title) },
                    )
                }
            }

            // Verbose Enable/Disable warning
            if (pagerState.currentPage == 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Verbose Logging", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isVerboseEnabled,
                        onCheckedChange = { viewModel.toggleVerbose(it) },
                    )
                }
                HorizontalDivider()
            }

            // Log Content
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val logs = if (page == 0) moduleLogs else verboseLogs
                    LogViewer(logs = logs, wordWrap = wordWrapEnabled)
                }
            }
        }
    }
}

@Composable
private fun LogViewer(logs: List<String>, wordWrap: Boolean) {
    // If word wrap is false, we allow horizontal scrolling on the LazyColumn
    val horizontalScrollState = rememberScrollState()

    // Apply horizontal scrolling if wordWrap is disabled
    val modifier =
        if (!wordWrap) {
            Modifier.fillMaxSize().horizontalScroll(horizontalScrollState)
        } else {
            Modifier.fillMaxSize()
        }

    SelectionContainer {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            items(logs) { line ->
                Text(
                    text = line,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    softWrap = wordWrap,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
            // Bottom padding to avoid navigation bar collision
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}
