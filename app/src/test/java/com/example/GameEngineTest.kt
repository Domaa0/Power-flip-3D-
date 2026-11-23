package com.example

import com.example.game.ControlMode
import com.example.game.FlipSkin
import com.example.game.GameEngine
import com.example.game.GamePhase
import com.example.game.RoomTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    @Test
    fun classicSingleTapLandsCleanlyOnFirstPlatform() {
        val engine = GameEngine(Random(42))
        engine.startNewGame()
        assertEquals(FlipSkin.PENIS_CLASSIC, engine.getUiState().selectedSkin)
        assertEquals(ControlMode.CLASSIC_DOUBLE_TAP, engine.getUiState().controlMode)

        assertTrue(engine.onTapInput())
        assertEquals(GamePhase.FLYING, engine.getUiState().phase)
        assertTrue(engine.getUiState().airFlipAvailable)

        for (i in 0 until 150) {
            engine.step(0.016f)
            val st = engine.getUiState()
            if (st.phase == GamePhase.AIMING || st.phase == GamePhase.WOBBLING) {
                break
            }
        }

        val afterLand = engine.getUiState()
        assertTrue("Expected score >= 1 on first classic single-tap jump, phase=${afterLand.phase}", afterLand.score >= 1)
        assertNotEquals(GamePhase.GAME_OVER, afterLand.phase)
    }

    @Test
    fun airFlipBoostExtendsJumpForDoubleFlip() {
        val engine = GameEngine(Random(42))
        engine.startNewGame()
        assertTrue(engine.onTapInput())

        // Simulate 0.4s to apex and perform mid-air second tap
        repeat(25) { engine.step(0.016f) }
        assertTrue(engine.onTapInput())
        assertEquals(false, engine.getUiState().airFlipAvailable)

        val snapMid = engine.getRenderSnapshot()
        assertTrue("Should advance forward past x=1.1 at apex", snapMid.objX > 1.1f)

        repeat(35) { engine.step(0.016f) }
        val snapExtended = engine.getRenderSnapshot()
        assertTrue("Double flip boost should carry past x=2.8", snapExtended.objX > 2.8f)
    }

    @Test
    fun powerBarModeOscillatesAndScoresInSweetZone() {
        val engine = GameEngine(Random(7))
        engine.syncPreferences(
            bestScore = 0,
            bestStreak = 0,
            soundEnabled = true,
            hapticsEnabled = true,
            skin = FlipSkin.WATER_BOTTLE,
            theme = RoomTheme.SUNNY_HOME,
            mode = ControlMode.POWER_BAR
        )
        engine.startNewGame()

        val sweet = engine.getUiState().tutorialSweetZone!!
        val targetMid = (sweet.start + sweet.endInclusive) * 0.5f

        var launched = false
        for (i in 0 until 240) {
            engine.step(0.016f)
            val p = engine.getUiState().powerLevel
            if (kotlin.math.abs(p - targetMid) < 0.03f) {
                launched = engine.onTapInput()
                break
            }
        }
        assertTrue("Should launch near sweet zone", launched)

        for (i in 0 until 150) {
            engine.step(0.016f)
            val st = engine.getUiState()
            if (st.phase == GamePhase.AIMING || st.phase == GamePhase.WOBBLING) {
                break
            }
        }

        val finalState = engine.getUiState()
        assertTrue("Expected score >= 1 after sweet-zone flip, phase=${finalState.phase}", finalState.score >= 1)
    }

    @Test
    fun furnitureDoesNotRepeatConsecutivelyAndSupportsPussySkin() {
        val engine = GameEngine(Random(123))
        engine.syncPreferences(
            bestScore = 0,
            bestStreak = 0,
            soundEnabled = true,
            hapticsEnabled = true,
            skin = FlipSkin.PUSSY_CLASSIC,
            theme = RoomTheme.SUNNY_HOME,
            mode = ControlMode.CLASSIC_DOUBLE_TAP
        )
        engine.startNewGame()
        assertEquals(FlipSkin.PUSSY_CLASSIC, engine.getUiState().selectedSkin)

        val plats = engine.getRenderSnapshot().platforms
        assertTrue("Expected at least 8 platforms buffered ahead, got ${plats.size}", plats.size >= 8)
        for (start in 0..(plats.size - 6)) {
            val windowStyles = plats.subList(start, start + 6).map { it.style }
            assertEquals(
                "6 consecutive furniture platforms starting at $start must all be distinct: $windowStyles",
                6,
                windowStyles.toSet().size
            )
        }
    }
}
