package org.matrix.vector.manager.ui.screens.repo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.matrix.vector.manager.data.model.OnlineModule
import org.matrix.vector.manager.data.repository.RepoRepository

sealed interface RepoDetailsUiState {
    data object Loading : RepoDetailsUiState

    data class Success(val module: OnlineModule) : RepoDetailsUiState

    data object Error : RepoDetailsUiState
}

class RepoDetailsViewModel(
    private val packageName: String,
    private val repoRepository: RepoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RepoDetailsUiState>(RepoDetailsUiState.Loading)
    val uiState: StateFlow<RepoDetailsUiState> = _uiState.asStateFlow()

    init {
        fetchDetails()
    }

    fun fetchDetails() {
        viewModelScope.launch {
            _uiState.value = RepoDetailsUiState.Loading
            val details = repoRepository.getModuleDetails(packageName)
            if (details != null) {
                _uiState.value = RepoDetailsUiState.Success(details)
            } else {
                _uiState.value = RepoDetailsUiState.Error
            }
        }
    }
}
