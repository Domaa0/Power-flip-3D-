package com.example.game

import androidx.compose.ui.graphics.Color

enum class FlipShape {
    PENIS_SKIN,
    PUSSY_SKIN,
    WATER_BOTTLE,
    SODA_BOTTLE,
    TROPHY_BOTTLE
}

enum class ControlMode(val id: String, val displayName: String) {
    CLASSIC_DOUBLE_TAP("classic_double_tap", "Classic Tap + Air Flip"),
    POWER_BAR("power_bar", "Moving Power Bar");

    companion object {
        fun fromId(id: String): ControlMode = entries.find { it.id == id } ?: CLASSIC_DOUBLE_TAP
    }
}

enum class FlipSkin(
    val id: String,
    val displayName: String,
    val shape: FlipShape,
    val bodyRgb: FloatArray,
    val capRgb: FloatArray,
    val accentRgb: FloatArray,
    val previewColor: Color,
    val accentPreviewColor: Color
) {
    PENIS_CLASSIC(
        id = "penis_classic",
        displayName = "Penis Classic",
        shape = FlipShape.PENIS_SKIN,
        bodyRgb = floatArrayOf(1.00f, 0.52f, 0.48f),
        capRgb = floatArrayOf(0.98f, 0.34f, 0.44f),
        accentRgb = floatArrayOf(1.00f, 0.80f, 0.74f),
        previewColor = Color(0xFFFF857A),
        accentPreviewColor = Color(0xFFFA5770)
    ),
    PUSSY_CLASSIC(
        id = "pussy_classic",
        displayName = "Pussy Peach",
        shape = FlipShape.PUSSY_SKIN,
        bodyRgb = floatArrayOf(1.00f, 0.62f, 0.60f),
        capRgb = floatArrayOf(0.96f, 0.28f, 0.46f),
        accentRgb = floatArrayOf(0.98f, 0.38f, 0.52f),
        previewColor = Color(0xFFFF9E99),
        accentPreviewColor = Color(0xFFF54775)
    ),
    PUSSY_BERRY(
        id = "pussy_berry",
        displayName = "Pussy Berry",
        shape = FlipShape.PUSSY_SKIN,
        bodyRgb = floatArrayOf(0.96f, 0.45f, 0.74f),
        capRgb = floatArrayOf(0.82f, 0.16f, 0.54f),
        accentRgb = floatArrayOf(1.00f, 0.68f, 0.86f),
        previewColor = Color(0xFFF573BD),
        accentPreviewColor = Color(0xFFD1298A)
    ),
    WATER_BOTTLE(
        id = "water_bottle",
        displayName = "Water Bottle",
        shape = FlipShape.WATER_BOTTLE,
        bodyRgb = floatArrayOf(0.56f, 0.88f, 0.99f),
        capRgb = floatArrayOf(0.09f, 0.46f, 0.95f),
        accentRgb = floatArrayOf(0.96f, 0.98f, 1.00f),
        previewColor = Color(0xFF8FE0FC),
        accentPreviewColor = Color(0xFF1775F2)
    ),
    SODA_BOTTLE(
        id = "soda_bottle",
        displayName = "Berry Soda",
        shape = FlipShape.SODA_BOTTLE,
        bodyRgb = floatArrayOf(0.28f, 0.14f, 0.22f),
        capRgb = floatArrayOf(0.96f, 0.22f, 0.32f),
        accentRgb = floatArrayOf(1.00f, 0.92f, 0.88f),
        previewColor = Color(0xFF6B2848),
        accentPreviewColor = Color(0xFFF53852)
    ),
    TROPHY_BOTTLE(
        id = "trophy_bottle",
        displayName = "Gold Bottle",
        shape = FlipShape.TROPHY_BOTTLE,
        bodyRgb = floatArrayOf(0.18f, 0.54f, 0.34f),
        capRgb = floatArrayOf(1.00f, 0.80f, 0.24f),
        accentRgb = floatArrayOf(1.00f, 0.86f, 0.38f),
        previewColor = Color(0xFF2E8A57),
        accentPreviewColor = Color(0xFFFFCC3D)
    ),
    PENIS_BUBBLEGUM(
        id = "penis_bubblegum",
        displayName = "Penis Pink",
        shape = FlipShape.PENIS_SKIN,
        bodyRgb = floatArrayOf(0.98f, 0.44f, 0.78f),
        capRgb = floatArrayOf(0.84f, 0.22f, 0.62f),
        accentRgb = floatArrayOf(1.00f, 0.82f, 0.94f),
        previewColor = Color(0xFFFA70C7),
        accentPreviewColor = Color(0xFFD6389E)
    ),
    PENIS_GOLD(
        id = "penis_gold",
        displayName = "Penis Gold",
        shape = FlipShape.PENIS_SKIN,
        bodyRgb = floatArrayOf(1.00f, 0.78f, 0.22f),
        capRgb = floatArrayOf(1.00f, 0.60f, 0.10f),
        accentRgb = floatArrayOf(1.00f, 0.95f, 0.70f),
        previewColor = Color(0xFFFFC738),
        accentPreviewColor = Color(0xFFFF991A)
    ),
    PENIS_NEON(
        id = "penis_neon",
        displayName = "Penis Neon",
        shape = FlipShape.PENIS_SKIN,
        bodyRgb = floatArrayOf(0.45f, 0.36f, 1.00f),
        capRgb = floatArrayOf(0.18f, 0.92f, 0.84f),
        accentRgb = floatArrayOf(0.86f, 0.82f, 1.00f),
        previewColor = Color(0xFF735CFF),
        accentPreviewColor = Color(0xFF2EEBD6)
    ),
    PENIS_MINT(
        id = "penis_mint",
        displayName = "Penis Mint",
        shape = FlipShape.PENIS_SKIN,
        bodyRgb = floatArrayOf(0.28f, 0.90f, 0.72f),
        capRgb = floatArrayOf(0.14f, 0.70f, 0.68f),
        accentRgb = floatArrayOf(0.84f, 1.00f, 0.94f),
        previewColor = Color(0xFF47E6B8),
        accentPreviewColor = Color(0xFF24B3AD)
    );

    companion object {
        fun fromId(id: String): FlipSkin = entries.find { it.id == id } ?: PENIS_CLASSIC
    }
}

enum class RoomTheme(
    val id: String,
    val displayName: String,
    val bgTopRgb: FloatArray,
    val bgBottomRgb: FloatArray,
    val floorRgb: FloatArray,
    val wallAccentRgb: FloatArray,
    val previewTop: Color,
    val previewBottom: Color
) {
    SUNNY_HOME(
        id = "sunny_home",
        displayName = "Sunny Home",
        bgTopRgb = floatArrayOf(0.46f, 0.78f, 0.97f),
        bgBottomRgb = floatArrayOf(0.99f, 0.94f, 0.86f),
        floorRgb = floatArrayOf(0.84f, 0.69f, 0.52f),
        wallAccentRgb = floatArrayOf(0.98f, 0.95f, 0.90f),
        previewTop = Color(0xFF75C7F7),
        previewBottom = Color(0xFFFCEFD9)
    ),
    SUNSET_ARCADE(
        id = "sunset_arcade",
        displayName = "Arcade Indigo",
        bgTopRgb = floatArrayOf(0.11f, 0.09f, 0.23f),
        bgBottomRgb = floatArrayOf(0.22f, 0.15f, 0.38f),
        floorRgb = floatArrayOf(0.14f, 0.12f, 0.28f),
        wallAccentRgb = floatArrayOf(0.32f, 0.24f, 0.56f),
        previewTop = Color(0xFF1C173B),
        previewBottom = Color(0xFF382661)
    ),
    MIDNIGHT_NEON(
        id = "midnight_neon",
        displayName = "Cyber Studio",
        bgTopRgb = floatArrayOf(0.05f, 0.06f, 0.12f),
        bgBottomRgb = floatArrayOf(0.11f, 0.17f, 0.28f),
        floorRgb = floatArrayOf(0.08f, 0.10f, 0.18f),
        wallAccentRgb = floatArrayOf(0.18f, 0.34f, 0.48f),
        previewTop = Color(0xFF0D0F1F),
        previewBottom = Color(0xFF1C2B47)
    );

    companion object {
        fun fromId(id: String): RoomTheme = entries.find { it.id == id } ?: SUNNY_HOME
    }
}

enum class PlatformStyle {
    SIDE_TABLE,
    BOOK_STACK,
    OTTOMAN,
    SPEAKER_BOX,
    TV_STAND,
    KITCHEN_COUNTER,
    ARMCHAIR,
    MINI_FRIDGE,
    MICROWAVE_STAND,
    WASHING_MACHINE,
    BAR_STOOL,
    NIGHTSTAND_LAMP,
    TROPHY_PEDESTAL
}

data class Platform3D(
    val id: Int,
    val xCenter: Float,
    val yTop: Float,
    val width: Float,
    val depth: Float,
    val style: PlatformStyle,
    val primaryRgb: FloatArray,
    val accentRgb: FloatArray,
    val isGoldBonus: Boolean = false,
    val requiresDoubleFlip: Boolean = false
)

data class Particle3D(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    var rot: Float,
    var rotSpeed: Float,
    var size: Float,
    var life: Float,
    var maxLife: Float,
    val rgb: FloatArray
)

enum class GamePhase {
    TITLE,
    AIMING,
    FLYING,
    WOBBLING,
    CRASHING,
    GAME_OVER
}

data class LandingFeedback(
    val id: Long,
    val title: String,
    val pointsText: String,
    val isPerfect: Boolean,
    val isBonus: Boolean = false
)

data class GameRenderSnapshot(
    val phase: GamePhase,
    val objX: Float,
    val objY: Float,
    val objZ: Float,
    val flipAngleDeg: Float,
    val yawDeg: Float,
    val rollDeg: Float,
    val scaleX: Float,
    val scaleY: Float,
    val scaleZ: Float,
    val powerLevel: Float,
    val showPowerBarOnObject: Boolean,
    val camX: Float,
    val camY: Float,
    val screenShake: Float,
    val nearestSurfaceY: Float,
    val platforms: List<Platform3D>,
    val currentPlatformIndex: Int,
    val particles: List<Particle3D>,
    val skin: FlipSkin,
    val theme: RoomTheme
)

data class GameUiState(
    val phase: GamePhase = GamePhase.TITLE,
    val score: Int = 0,
    val bestScore: Int = 0,
    val streak: Int = 0,
    val maxRunStreak: Int = 0,
    val bestStreak: Int = 0,
    val perfectCount: Int = 0,
    val jumpsCompleted: Int = 0,
    val powerLevel: Float = 0.5f,
    val tutorialSweetZone: ClosedFloatingPointRange<Float>? = null,
    val airFlipAvailable: Boolean = false,
    val nextGapNeedsDoubleFlip: Boolean = false,
    val lastFeedback: LandingFeedback? = null,
    val isNewBest: Boolean = false,
    val failReason: String = "",
    val selectedSkin: FlipSkin = FlipSkin.PENIS_CLASSIC,
    val selectedTheme: RoomTheme = RoomTheme.SUNNY_HOME,
    val controlMode: ControlMode = ControlMode.CLASSIC_DOUBLE_TAP,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true
)
