package org.matrix.vector.manager.ui.screens.repo

import android.content.Intent
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.matrix.vector.manager.Graph
import org.matrix.vector.manager.data.model.OnlineModule
import org.matrix.vector.manager.data.model.Release

class RepoDetailsViewModelFactory(private val packageName: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RepoDetailsViewModel(packageName, Graph.repoRepository) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RepoDetailsScreen(packageName: String, onNavigateBack: () -> Unit) {
    val viewModel: RepoDetailsViewModel =
        viewModel(factory = RepoDetailsViewModelFactory(packageName))
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val title =
                            if (uiState is RepoDetailsUiState.Success)
                                (uiState as RepoDetailsUiState.Success).module.description
                            else "Details"

                        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
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
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is RepoDetailsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is RepoDetailsUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Failed to load module details.",
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.fetchDetails() }) { Text("Retry") }
                    }
                }
                is RepoDetailsUiState.Success -> {
                    RepoDetailsContent(module = state.module)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RepoDetailsContent(module: OnlineModule) {
    val tabs = listOf("Readme", "Releases", "Information")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title) },
                )
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> ReadmeTab(htmlContent = module.readmeHTML)
                1 -> ReleasesTab(releases = module.releases)
                2 -> InformationTab(module = module)
            }
        }
    }
}

@Composable
private fun ReadmeTab(htmlContent: String?) {
    if (htmlContent.isNullOrBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No Readme provided.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // Embeds the legacy WebView securely inside Compose to render GitHub HTML
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.apply {
                    javaScriptEnabled = false
                    domStorageEnabled = true
                    cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                    textZoom = 85
                }
            }
        },
        update = { webView ->
            // Minimal HTML wrapper for dark-mode friendly text
            val wrapper =
                """
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <style>
                        body { font-family: sans-serif; padding: 16px; color: #E0E0E0; line-height: 1.5; }
                        a { color: #8AB4F8; }
                        pre, code { background-color: #1E1E1E; padding: 4px; border-radius: 4px; }
                        img { max-width: 100%; height: auto; }
                    </style>
                </head>
                <body>$htmlContent</body>
                </html>
            """
                    .trimIndent()

            webView.loadDataWithBaseURL("https://github.com", wrapper, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun ReleasesTab(releases: List<Release>) {
    val context = LocalContext.current

    if (releases.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No releases found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(releases, key = { it.name ?: it.hashCode() }) { release ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = release.name ?: "Unknown Version",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = release.publishedAt?.take(10) ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!release.url.isNullOrBlank()) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Icon(
                                Icons.Rounded.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("View on GitHub")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InformationTab(module: OnlineModule) {
    val context = LocalContext.current

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Homepage
        if (!module.homepageUrl.isNullOrBlank()) {
            item {
                ListItem(
                    headlineContent = { Text("Homepage") },
                    supportingContent = { Text(module.homepageUrl) },
                    leadingContent = { Icon(Icons.Rounded.Language, contentDescription = null) },
                    modifier =
                        Modifier.clickable {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(module.homepageUrl))
                            )
                        },
                )
                HorizontalDivider()
            }
        }

        // Source URL
        if (!module.sourceUrl.isNullOrBlank()) {
            item {
                ListItem(
                    headlineContent = { Text("Source Code") },
                    supportingContent = { Text(module.sourceUrl) },
                    leadingContent = { Icon(Icons.Rounded.Code, contentDescription = null) },
                    modifier =
                        Modifier.clickable {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(module.sourceUrl))
                            )
                        },
                )
                HorizontalDivider()
            }
        }

        // Collaborators
        if (module.collaborators.isNotEmpty()) {
            item {
                ListItem(
                    headlineContent = { Text("Collaborators") },
                    supportingContent = {
                        val names =
                            module.collaborators
                                .mapNotNull { it.name ?: it.login }
                                .joinToString(", ")
                        Text(names)
                    },
                    leadingContent = { Icon(Icons.Rounded.Group, contentDescription = null) },
                )
                HorizontalDivider()
            }
        }
    }
}
