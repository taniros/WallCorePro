package com.offline.wallcorepro.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offline.wallcorepro.data.network.AiService
import com.offline.wallcorepro.data.repository.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiUiState(
    val generatedWish: String      = "",
    val wishHistory: List<String>  = emptyList(),
    val isLoading: Boolean         = false,
    val error: String?             = null,
    val selectedMood: String       = "Inspirational",
    val generateCount: Int         = 0,
    val showInterstitialTrigger: Boolean = false,
    // Persistent emotional tone (Soft / Deep / Romantic / Energetic / Funny)
    val selectedTone: String       = com.offline.wallcorepro.config.AppConfig.EmotionalTone.default.key,
    val userName: String           = "",
    val showEditDialog: Boolean    = false
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val aiService: AiService,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferenceManager.selectedTone.collect { tone ->
                _uiState.update { it.copy(selectedTone = tone) }
            }
        }
        viewModelScope.launch {
            preferenceManager.userName.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }
    }

    fun updateGeneratedWish(text: String) {
        _uiState.update { it.copy(generatedWish = text, showEditDialog = false) }
    }

    fun toggleEditDialog() {
        _uiState.update { it.copy(showEditDialog = !it.showEditDialog) }
    }

    fun onMoodSelected(mood: String) {
        _uiState.update { it.copy(selectedMood = mood) }
    }

    fun onToneSelected(toneKey: String) {
        _uiState.update { it.copy(selectedTone = toneKey) }
        viewModelScope.launch { preferenceManager.setSelectedTone(toneKey) }
    }

    fun generateWish(niche: String) {
        viewModelScope.launch {
            val newCount       = _uiState.value.generateCount + 1
            _uiState.update { it.copy(isLoading = true, error = null, generateCount = newCount) }
            val userName           = preferenceManager.userName.first()
            val selectedCategories = preferenceManager.selectedQuoteCategories.first()
            val result = aiService.generateWish(
                niche                = niche,
                mood                 = _uiState.value.selectedMood,
                userName             = userName,
                selectedCategoryKeys = selectedCategories,
                variationSeed        = newCount,
                tone                 = _uiState.value.selectedTone
            )
            result.onSuccess { wish ->
                val updatedHistory = (_uiState.value.wishHistory + wish).takeLast(5)
                val shouldShowInterstitial = newCount == com.offline.wallcorepro.config.AppConfig.INTERSTITIAL_AI_GENERATE
                _uiState.update { it.copy(
                    generatedWish = wish,
                    wishHistory   = updatedHistory,
                    isLoading     = false,
                    showInterstitialTrigger = shouldShowInterstitial
                ) }
            }.onFailure { e ->
                _uiState.update { it.copy(
                    isLoading = false,
                    error     = e.localizedMessage ?: "Failed to generate wish"
                ) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissInterstitialTrigger() {
        _uiState.update { it.copy(showInterstitialTrigger = false) }
    }
}
