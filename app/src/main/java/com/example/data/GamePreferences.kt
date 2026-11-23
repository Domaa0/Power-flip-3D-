package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.game.ControlMode
import com.example.game.FlipSkin
import com.example.game.RoomTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(name = "power_flip_prefs")

data class StoredGameSettings(
    val bestScore: Int = 0,
    val bestStreak: Int = 0,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val selectedSkin: FlipSkin = FlipSkin.PENIS_CLASSIC,
    val selectedTheme: RoomTheme = RoomTheme.SUNNY_HOME,
    val controlMode: ControlMode = ControlMode.CLASSIC_DOUBLE_TAP
)

class GamePreferences(private val context: Context) {

    private object Keys {
        val BEST_SCORE = intPreferencesKey("best_score")
        val BEST_STREAK = intPreferencesKey("best_streak")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SELECTED_SKIN = stringPreferencesKey("selected_skin_v2")
        val SELECTED_THEME = stringPreferencesKey("selected_theme_v2")
        val CONTROL_MODE = stringPreferencesKey("control_mode")
    }

    val settingsFlow: Flow<StoredGameSettings> = context.gameDataStore.data.map { prefs ->
        StoredGameSettings(
            bestScore = prefs[Keys.BEST_SCORE] ?: 0,
            bestStreak = prefs[Keys.BEST_STREAK] ?: 0,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            selectedSkin = FlipSkin.fromId(prefs[Keys.SELECTED_SKIN] ?: FlipSkin.PENIS_CLASSIC.id),
            selectedTheme = RoomTheme.fromId(prefs[Keys.SELECTED_THEME] ?: RoomTheme.SUNNY_HOME.id),
            controlMode = ControlMode.fromId(prefs[Keys.CONTROL_MODE] ?: ControlMode.CLASSIC_DOUBLE_TAP.id)
        )
    }

    suspend fun updateBestStats(score: Int, streak: Int) {
        context.gameDataStore.edit { prefs ->
            val currentBest = prefs[Keys.BEST_SCORE] ?: 0
            if (score > currentBest) {
                prefs[Keys.BEST_SCORE] = score
            }
            val currentBestStreak = prefs[Keys.BEST_STREAK] ?: 0
            if (streak > currentBestStreak) {
                prefs[Keys.BEST_STREAK] = streak
            }
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setSelectedSkin(skin: FlipSkin) {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.SELECTED_SKIN] = skin.id
        }
    }

    suspend fun setSelectedTheme(theme: RoomTheme) {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.SELECTED_THEME] = theme.id
        }
    }

    suspend fun setControlMode(mode: ControlMode) {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.CONTROL_MODE] = mode.id
        }
    }
}
