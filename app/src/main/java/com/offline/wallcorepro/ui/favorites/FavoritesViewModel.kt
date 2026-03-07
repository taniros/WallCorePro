package com.offline.wallcorepro.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offline.wallcorepro.data.repository.PreferenceManager
import com.offline.wallcorepro.domain.model.Wallpaper
import com.offline.wallcorepro.domain.usecase.GetFavoriteWallpapersUseCase
import com.offline.wallcorepro.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<Wallpaper> = emptyList(),
    val isLoading: Boolean = true,
    val isLocked: Boolean = false,      // biometric lock required
    val isUnlocked: Boolean = false     // user has authenticated this session
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoriteWallpapersUseCase: GetFavoriteWallpapersUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getFavoriteWallpapersUseCase(),
                preferenceManager.isFavoritesLocked
            ) { favorites, isLocked ->
                _uiState.value.copy(
                    favorites  = favorites,
                    isLoading  = false,
                    isLocked   = isLocked,
                    isUnlocked = _uiState.value.isUnlocked
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    /** Called after the user successfully authenticates with biometrics. */
    fun onBiometricSuccess() {
        _uiState.update { it.copy(isUnlocked = true) }
    }

    fun removeFromFavorites(id: String) = viewModelScope.launch {
        toggleFavoriteUseCase(id, false)
    }

    fun toggleFavorite(id: String, isFavorite: Boolean) = viewModelScope.launch {
        toggleFavoriteUseCase(id, isFavorite)
    }
}
