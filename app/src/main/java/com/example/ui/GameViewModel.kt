package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FlipRun
import com.example.data.GamePreferences
import com.example.data.GameRepository
import com.example.data.StoredGameSettings
import com.example.data.UserProfile
import com.example.game.ControlMode
import com.example.game.FlipSkin
import com.example.game.GameEngine
import com.example.game.GameEventListener
import com.example.game.GameUiState
import com.example.game.RoomTheme
import com.example.game.SoundEffects
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HapticEvent {
    data object Launch : HapticEvent
    data object LandPerfect : HapticEvent
    data object LandNormal : HapticEvent
    data object Crash : HapticEvent
}

sealed interface CloudUiState<out T> {
    data object Loading : CloudUiState<Nothing>
    data class Success<T>(val data: T) : CloudUiState<T>
    data class Error(val message: String) : CloudUiState<Nothing>
}

class GameViewModel(
    private val prefs: GamePreferences,
    private val repository: GameRepository,
    private val currentUserId: String,
    private val userDisplayName: String
) : ViewModel(), GameEventListener {

    val soundEffects = SoundEffects()
    val engine = GameEngine()

    private val _uiState = MutableStateFlow(engine.getUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _hapticEvents = MutableSharedFlow<HapticEvent>(extraBufferCapacity = 8)
    val hapticEvents: SharedFlow<HapticEvent> = _hapticEvents.asSharedFlow()

    private val _showCustomizeSheet = MutableStateFlow(false)
    val showCustomizeSheet: StateFlow<Boolean> = _showCustomizeSheet.asStateFlow()

    private val _showHelpDialog = MutableStateFlow(false)
    val showHelpDialog: StateFlow<Boolean> = _showHelpDialog.asStateFlow()

    private val _showCloudProfileSheet = MutableStateFlow(false)
    val showCloudProfileSheet: StateFlow<Boolean> = _showCloudProfileSheet.asStateFlow()

    private val _cloudSyncError = MutableStateFlow<String?>(null)
    val cloudSyncError: StateFlow<String?> = _cloudSyncError.asStateFlow()

    private var latestLocalSettings = StoredGameSettings()
    private var hasSyncedInitialCloudProfile = false

    val cloudProfileState: StateFlow<CloudUiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .map<UserProfile?, CloudUiState<UserProfile?>> { CloudUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing cloud profile", rootError)
                emit(CloudUiState.Error(rootError.localizedMessage ?: "Failed to load cloud profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CloudUiState.Loading
            )

    val recentRunsState: StateFlow<CloudUiState<List<FlipRun>>> =
        repository.observeRecentRuns(currentUserId)
            .map<List<FlipRun>, CloudUiState<List<FlipRun>>> { CloudUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing recent runs", rootError)
                emit(CloudUiState.Error(rootError.localizedMessage ?: "Failed to load run history"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CloudUiState.Loading
            )

    init {
        engine.eventListener = this

        viewModelScope.launch {
            prefs.settingsFlow.collect { stored ->
                latestLocalSettings = stored
                soundEffects.enabled = stored.soundEnabled
                engine.syncPreferences(
                    bestScore = stored.bestScore,
                    bestStreak = stored.bestStreak,
                    soundEnabled = stored.soundEnabled,
                    hapticsEnabled = stored.hapticsEnabled,
                    skin = stored.selectedSkin,
                    theme = stored.selectedTheme,
                    mode = stored.controlMode
                )
                refreshUiState()
            }
        }

        viewModelScope.launch {
            val initialLocal = prefs.settingsFlow.first()
            latestLocalSettings = initialLocal
            val result = repository.ensureUserProfile(
                userId = currentUserId,
                displayName = userDisplayName,
                localSettings = initialLocal
            )
            result.onFailure { err ->
                _cloudSyncError.value = err.localizedMessage ?: "Cloud sync unavailable"
            }
        }

        viewModelScope.launch {
            cloudProfileState.collect { state ->
                if (state is CloudUiState.Success) {
                    val profile = state.data ?: return@collect
                    _cloudSyncError.value = null

                    val cloudBestScore = profile.bestScore.toInt().coerceAtLeast(0)
                    val cloudBestStreak = profile.bestStreak.toInt().coerceAtLeast(0)
                    if (cloudBestScore > latestLocalSettings.bestScore ||
                        cloudBestStreak > latestLocalSettings.bestStreak
                    ) {
                        prefs.updateBestStats(cloudBestScore, cloudBestStreak)
                    }

                    if (!hasSyncedInitialCloudProfile) {
                        hasSyncedInitialCloudProfile = true
                        val cloudSkin = profile.resolvedSkin()
                        val cloudTheme = profile.resolvedTheme()
                        val cloudMode = profile.resolvedControlMode()
                        if (cloudSkin != latestLocalSettings.selectedSkin) {
                            prefs.setSelectedSkin(cloudSkin)
                        }
                        if (cloudTheme != latestLocalSettings.selectedTheme) {
                            prefs.setSelectedTheme(cloudTheme)
                        }
                        if (cloudMode != latestLocalSettings.controlMode) {
                            prefs.setControlMode(cloudMode)
                        }
                        if (profile.soundEnabled != latestLocalSettings.soundEnabled) {
                            prefs.setSoundEnabled(profile.soundEnabled)
                        }
                        if (profile.hapticsEnabled != latestLocalSettings.hapticsEnabled) {
                            prefs.setHapticsEnabled(profile.hapticsEnabled)
                        }
                    }
                }
            }
        }
    }

    fun refreshUiState() {
        _uiState.value = engine.getUiState()
    }

    fun onStartGame() {
        soundEffects.playClick()
        engine.startNewGame()
        refreshUiState()
    }

    fun onViewportTap() {
        if (engine.onTapInput()) {
            refreshUiState()
        }
    }

    fun onToggleSound() {
        val next = !_uiState.value.soundEnabled
        soundEffects.enabled = next
        if (next) soundEffects.playClick()
        viewModelScope.launch {
            prefs.setSoundEnabled(next)
            syncCurrentPreferencesToCloud(soundEnabled = next)
        }
    }

    fun onToggleHaptics() {
        val next = !_uiState.value.hapticsEnabled
        soundEffects.playClick()
        viewModelScope.launch {
            prefs.setHapticsEnabled(next)
            syncCurrentPreferencesToCloud(hapticsEnabled = next)
        }
    }

    fun onSelectSkin(skin: FlipSkin) {
        soundEffects.playClick()
        engine.triggerPreviewBounce()
        viewModelScope.launch {
            prefs.setSelectedSkin(skin)
            syncCurrentPreferencesToCloud(skin = skin)
        }
    }

    fun onSelectTheme(theme: RoomTheme) {
        soundEffects.playClick()
        viewModelScope.launch {
            prefs.setSelectedTheme(theme)
            syncCurrentPreferencesToCloud(theme = theme)
        }
    }

    fun onSelectControlMode(mode: ControlMode) {
        soundEffects.playClick()
        viewModelScope.launch {
            prefs.setControlMode(mode)
            syncCurrentPreferencesToCloud(mode = mode)
        }
    }

    fun openCustomizeSheet(open: Boolean) {
        soundEffects.playClick()
        _showCustomizeSheet.value = open
    }

    fun openHelpDialog(open: Boolean) {
        soundEffects.playClick()
        _showHelpDialog.value = open
    }

    fun openCloudProfileSheet(open: Boolean) {
        soundEffects.playClick()
        _showCloudProfileSheet.value = open
    }

    fun onDeleteRun(runId: String) {
        soundEffects.playClick()
        viewModelScope.launch {
            repository.deleteRun(currentUserId, runId).onFailure { err ->
                _cloudSyncError.value = err.localizedMessage ?: "Could not delete run"
            }
        }
    }

    fun dismissCloudSyncError() {
        _cloudSyncError.value = null
    }

    private suspend fun syncCurrentPreferencesToCloud(
        skin: FlipSkin = _uiState.value.selectedSkin,
        theme: RoomTheme = _uiState.value.selectedTheme,
        mode: ControlMode = _uiState.value.controlMode,
        soundEnabled: Boolean = _uiState.value.soundEnabled,
        hapticsEnabled: Boolean = _uiState.value.hapticsEnabled
    ) {
        repository.syncPreferences(
            userId = currentUserId,
            displayName = userDisplayName,
            skin = skin,
            theme = theme,
            mode = mode,
            soundEnabled = soundEnabled,
            hapticsEnabled = hapticsEnabled,
            fallbackSettings = latestLocalSettings
        ).onFailure { err ->
            _cloudSyncError.value = err.localizedMessage ?: "Failed to sync preferences"
        }
    }

    override fun onLaunch(power: Float) {
        soundEffects.playLaunch(power)
        if (_uiState.value.hapticsEnabled) {
            _hapticEvents.tryEmit(HapticEvent.Launch)
        }
    }

    override fun onAirFlip() {
        soundEffects.playAirFlip()
        if (_uiState.value.hapticsEnabled) {
            _hapticEvents.tryEmit(HapticEvent.Launch)
        }
    }

    override fun onLand(isPerfect: Boolean, streak: Int, score: Int) {
        soundEffects.playLand(isPerfect, streak)
        if (_uiState.value.hapticsEnabled) {
            _hapticEvents.tryEmit(if (isPerfect) HapticEvent.LandPerfect else HapticEvent.LandNormal)
        }
        val prevBestScore = latestLocalSettings.bestScore
        val prevBestStreak = latestLocalSettings.bestStreak
        viewModelScope.launch {
            prefs.updateBestStats(score, streak)
            if (score > prevBestScore || streak > prevBestStreak) {
                repository.updateBestStats(
                    userId = currentUserId,
                    displayName = userDisplayName,
                    score = score,
                    streak = streak,
                    fallbackSettings = latestLocalSettings
                )
            }
        }
    }

    override fun onWobbleSave(score: Int) {
        soundEffects.playWobbleSave()
        if (_uiState.value.hapticsEnabled) {
            _hapticEvents.tryEmit(HapticEvent.LandNormal)
        }
        val prevBestScore = latestLocalSettings.bestScore
        viewModelScope.launch {
            prefs.updateBestStats(score, 0)
            if (score > prevBestScore) {
                repository.updateBestStats(
                    userId = currentUserId,
                    displayName = userDisplayName,
                    score = score,
                    streak = 0,
                    fallbackSettings = latestLocalSettings
                )
            }
        }
    }

    override fun onCrash(finalScore: Int, maxRunStreak: Int) {
        soundEffects.playCrash()
        if (_uiState.value.hapticsEnabled) {
            _hapticEvents.tryEmit(HapticEvent.Crash)
        }
        val snapshotState = engine.getUiState()
        viewModelScope.launch {
            prefs.updateBestStats(finalScore, maxRunStreak)
            repository.recordCompletedRun(
                userId = currentUserId,
                displayName = userDisplayName,
                score = finalScore,
                maxStreak = maxRunStreak,
                perfectCount = snapshotState.perfectCount,
                jumpsCompleted = snapshotState.jumpsCompleted,
                skin = snapshotState.selectedSkin,
                theme = snapshotState.selectedTheme,
                controlMode = snapshotState.controlMode,
                failReason = snapshotState.failReason,
                fallbackSettings = latestLocalSettings
            ).onFailure { err ->
                _cloudSyncError.value = err.localizedMessage ?: "Failed to record cloud run"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundEffects.release()
    }

    private companion object {
        const val TAG = "GameVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
