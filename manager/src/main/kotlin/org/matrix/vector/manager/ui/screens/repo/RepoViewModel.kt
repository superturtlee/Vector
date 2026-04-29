package org.matrix.vector.manager.ui.screens.repo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.matrix.vector.manager.data.model.OnlineModule
import org.matrix.vector.manager.data.repository.RepoRepository

class RepoViewModel(private val repoRepository: RepoRepository) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val isRefreshing: StateFlow<Boolean> = repoRepository.isRefreshing

    // Combine the raw list from the repository with the search query
    val filteredModules: StateFlow<List<OnlineModule>> =
        combine(repoRepository.onlineModules, searchQuery) { modules, query ->
                if (query.isBlank()) {
                        modules
                    } else {
                        modules.filter {
                            it.description.contains(query, ignoreCase = true) ||
                                it.name.contains(query, ignoreCase = true) ||
                                (it.summary?.contains(query, ignoreCase = true) == true)
                        }
                    }
                    .sortedBy { it.description.lowercase() } // Sort by name alphabetically
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    init {
        // Fetch on startup if empty
        if (repoRepository.onlineModules.value.isEmpty()) {
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch { repoRepository.refreshModules() }
    }
}
