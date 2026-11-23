package com.example.data

import androidx.annotation.Keep
import com.example.game.ControlMode
import com.example.game.FlipSkin
import com.example.game.RoomTheme
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Flipper",
    val bestScore: Long = 0L,
    val bestStreak: Long = 0L,
    val totalRuns: Long = 0L,
    val totalPerfects: Long = 0L,
    val selectedSkinId: String = FlipSkin.PENIS_CLASSIC.id,
    val selectedThemeId: String = RoomTheme.SUNNY_HOME.id,
    val controlModeId: String = ControlMode.CLASSIC_DOUBLE_TAP.id,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedSkin(): FlipSkin = FlipSkin.fromId(selectedSkinId)
    fun resolvedTheme(): RoomTheme = RoomTheme.fromId(selectedThemeId)
    fun resolvedControlMode(): ControlMode = ControlMode.fromId(controlModeId)
}

@Keep
@IgnoreExtraProperties
data class FlipRun(
    @DocumentId val id: String = "",
    val userId: String = "",
    val score: Long = 0L,
    val maxStreak: Long = 0L,
    val perfectCount: Long = 0L,
    val jumpsCompleted: Long = 0L,
    val skinId: String = FlipSkin.PENIS_CLASSIC.id,
    val themeId: String = RoomTheme.SUNNY_HOME.id,
    val controlModeId: String = ControlMode.CLASSIC_DOUBLE_TAP.id,
    val failReason: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedSkin(): FlipSkin = FlipSkin.fromId(skinId)
    fun resolvedTheme(): RoomTheme = RoomTheme.fromId(themeId)
    fun resolvedControlMode(): ControlMode = ControlMode.fromId(controlModeId)
}
