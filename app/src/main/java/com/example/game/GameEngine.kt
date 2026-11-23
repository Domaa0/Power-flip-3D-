package com.example.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

interface GameEventListener {
    fun onLaunch(power: Float)
    fun onAirFlip()
    fun onLand(isPerfect: Boolean, streak: Int, score: Int)
    fun onWobbleSave(score: Int)
    fun onCrash(finalScore: Int, maxRunStreak: Int)
}

class GameEngine(
    private val random: Random = Random.Default
) {
    companion object {
        const val GRAVITY = -22.0f
        const val FLOOR_Y = -3.4f
        const val POWER_MIN = 0.12f
        const val POWER_MAX = 1.00f
        const val PERFECT_ANGLE_DEG = 28f
        const val SAVE_ANGLE_DEG = 56f

        // Authentic Bottle Flip 3D single & double tap constants (calibrated for 5+ visible furniture layout)
        const val CLASSIC_VX = 3.25f
        const val CLASSIC_VY = 9.20f
        const val AIR_FLIP_VX = 3.35f
    }

    private val lock = Any()

    var eventListener: GameEventListener? = null

    private var phase = GamePhase.TITLE
    private var score = 0
    private var bestScore = 0
    private var streak = 0
    private var maxRunStreak = 0
    private var bestStreak = 0
    private var perfectCount = 0
    private var jumpsCompleted = 0
    private var isNewBest = false
    private var failReason = ""

    private var selectedSkin = FlipSkin.PENIS_CLASSIC
    private var selectedTheme = RoomTheme.SUNNY_HOME
    private var controlMode = ControlMode.CLASSIC_DOUBLE_TAP
    private var soundEnabled = true
    private var hapticsEnabled = true

    // Physics state
    private var objX = 0f
    private var objY = 0f
    private var objZ = 0f
    private var vx = 0f
    private var vy = 0f
    private var flipAngleDeg = 0f
    private var angularVelDeg = 0f
    private var yawDeg = 0f
    private var yawVelDeg = 0f
    private var rollDeg = 0f
    private var rollVelDeg = 0f

    // Squash & stretch animation
    private var scaleX = 1f
    private var scaleY = 1f
    private var scaleZ = 1f
    private var wobbleTimer = 0f
    private var wobbleStartAngle = 0f
    private var floorBounces = 0
    private var crashTimer = 0f

    // Moving power bar state (when ControlMode.POWER_BAR is active)
    private var oscillatorClock = 0f
    private var powerLevel = 0.5f
    private var airFlipAvailable = false

    // Camera state
    private var camX = 1.8f
    private var camY = 0.6f
    private var screenShake = 0f

    // Platforms and particles
    private val platforms = mutableListOf<Platform3D>()
    private val furnitureDeck = ArrayDeque<PlatformStyle>()
    private var lastNonGoldStyle: PlatformStyle = PlatformStyle.SIDE_TABLE
    private var currentPlatformIndex = 0
    private var nextPlatformId = 0
    private val particles = mutableListOf<Particle3D>()

    private var feedbackCounter = 0L
    private var lastFeedback: LandingFeedback? = null
    private var feedbackTimer = 0f

    private var tutorialSweetZone: ClosedFloatingPointRange<Float>? = null

    init {
        resetWorld(toTitle = true)
    }

    fun syncPreferences(
        bestScore: Int,
        bestStreak: Int,
        soundEnabled: Boolean,
        hapticsEnabled: Boolean,
        skin: FlipSkin,
        theme: RoomTheme,
        mode: ControlMode
    ) {
        synchronized(lock) {
            this.bestScore = max(this.bestScore, bestScore)
            this.bestStreak = max(this.bestStreak, bestStreak)
            this.soundEnabled = soundEnabled
            this.hapticsEnabled = hapticsEnabled
            this.selectedSkin = skin
            this.selectedTheme = theme
            this.controlMode = mode
            updateTutorialSweetZone()
        }
    }

    fun startNewGame() {
        synchronized(lock) {
            resetWorld(toTitle = false)
        }
    }

    fun returnToTitle() {
        synchronized(lock) {
            resetWorld(toTitle = true)
        }
    }

    fun triggerPreviewBounce() {
        synchronized(lock) {
            if (phase == GamePhase.TITLE || phase == GamePhase.AIMING) {
                scaleX = 1.24f
                scaleY = 0.74f
                scaleZ = 1.24f
                yawDeg = (yawDeg + 35f) % 360f
            }
        }
    }

    private fun resetWorld(toTitle: Boolean) {
        phase = if (toTitle) GamePhase.TITLE else GamePhase.AIMING
        score = 0
        streak = 0
        maxRunStreak = 0
        perfectCount = 0
        jumpsCompleted = 0
        isNewBest = false
        failReason = ""
        lastFeedback = null
        feedbackTimer = 0f

        platforms.clear()
        furnitureDeck.clear()
        particles.clear()
        nextPlatformId = 0
        currentPlatformIndex = 0
        lastNonGoldStyle = PlatformStyle.SIDE_TABLE

        // Starting kitchen/dining table
        platforms.add(
            Platform3D(
                id = nextPlatformId++,
                xCenter = 0f,
                yTop = 0f,
                width = 1.95f,
                depth = 1.95f,
                style = PlatformStyle.SIDE_TABLE,
                primaryRgb = floatArrayOf(0.95f, 0.78f, 0.56f),
                accentRgb = floatArrayOf(0.58f, 0.39f, 0.25f)
            )
        )
        repeat(9) {
            generateNextPlatform()
        }

        objX = platforms[0].xCenter
        objY = platforms[0].yTop
        objZ = 0f
        vx = 0f
        vy = 0f
        flipAngleDeg = 0f
        angularVelDeg = 0f
        yawDeg = 0f
        yawVelDeg = 0f
        rollDeg = 0f
        rollVelDeg = 0f
        scaleX = 1f
        scaleY = 1f
        scaleZ = 1f
        oscillatorClock = 0.35f
        powerLevel = 0.45f
        airFlipAvailable = false
        screenShake = 0f
        floorBounces = 0
        crashTimer = 0f

        camX = objX
        camY = objY + 0.2f

        updateTutorialSweetZone()
    }

    private fun updateTutorialSweetZone() {
        if (controlMode != ControlMode.POWER_BAR) {
            tutorialSweetZone = null
            return
        }
        tutorialSweetZone = if (jumpsCompleted < 2 && platforms.size > currentPlatformIndex + 1) {
            val current = platforms[currentPlatformIndex]
            val next = platforms[currentPlatformIndex + 1]
            val ideal = findIdealPower(current, next)
            val low = (ideal - 0.08f).coerceIn(POWER_MIN, POWER_MAX)
            val high = (ideal + 0.08f).coerceIn(POWER_MIN, POWER_MAX)
            low..high
        } else {
            null
        }
    }

    private fun findIdealPower(from: Platform3D, to: Platform3D): Float {
        val dy = to.yTop - from.yTop
        var bestP = 0.5f
        var bestErr = Float.MAX_VALUE
        for (i in 0..80) {
            val p = POWER_MIN + (POWER_MAX - POWER_MIN) * (i / 80f)
            val testVx = 2.8f + p * 4.4f
            val testVy = 6.2f + p * 7.8f
            val a = 0.5f * GRAVITY
            val disc = testVy * testVy - 4f * a * (-dy)
            if (disc >= 0f) {
                val tDesc = (-testVy - sqrt(disc)) / (2f * a)
                if (tDesc > 0.1f) {
                    val landX = from.xCenter + testVx * tDesc
                    val err = abs(landX - to.xCenter)
                    if (err < bestErr) {
                        bestErr = err
                        bestP = p
                    }
                }
            }
        }
        return bestP
    }

    private fun drawNextFurnitureStyle(): PlatformStyle {
        if (furnitureDeck.isEmpty()) {
            val allStyles = PlatformStyle.entries
                .filter { it != PlatformStyle.TROPHY_PEDESTAL }
                .shuffled(random)
                .toMutableList()
            if (allStyles.first() == lastNonGoldStyle && allStyles.size > 1) {
                val first = allStyles.removeAt(0)
                allStyles.add(first)
            }
            furnitureDeck.addAll(allStyles)
        }
        val picked = furnitureDeck.removeFirst()
        lastNonGoldStyle = picked
        return picked
    }

    private fun generateNextPlatform() {
        val prev = platforms.last()
        val idx = nextPlatformId
        val difficulty = (idx / 24f).coerceIn(0f, 1f)

        val isDoubleFlipGap = idx >= 3 && (idx % 3 == 0 || random.nextFloat() < (0.28f + difficulty * 0.22f))

        val dx = if (isDoubleFlipGap) {
            3.65f + random.nextFloat() * 0.55f
        } else {
            2.45f + random.nextFloat() * 0.55f
        }

        val rawDy = if (isDoubleFlipGap) {
            random.nextFloat() * 0.85f - 0.25f
        } else {
            random.nextFloat() * 0.68f - 0.34f
        }
        val targetY = (prev.yTop + rawDy).coerceIn(-0.95f, 1.85f)

        val width = (1.82f - difficulty * 0.35f + (random.nextFloat() * 0.20f - 0.08f))
            .coerceIn(1.38f, 1.98f)
        val depth = 1.75f + random.nextFloat() * 0.28f

        val isGold = idx > 0 && idx % 7 == 0
        val style = if (isGold) PlatformStyle.TROPHY_PEDESTAL else drawNextFurnitureStyle()

        val (primary, accent) = when (style) {
            PlatformStyle.TROPHY_PEDESTAL -> floatArrayOf(1.00f, 0.78f, 0.22f) to floatArrayOf(1.00f, 0.94f, 0.65f)
            PlatformStyle.SIDE_TABLE -> floatArrayOf(0.92f, 0.74f, 0.54f) to floatArrayOf(0.55f, 0.36f, 0.22f)
            PlatformStyle.BOOK_STACK -> floatArrayOf(0.32f, 0.74f, 0.94f) to floatArrayOf(0.96f, 0.42f, 0.46f)
            PlatformStyle.OTTOMAN -> floatArrayOf(0.28f, 0.86f, 0.70f) to floatArrayOf(0.20f, 0.60f, 0.50f)
            PlatformStyle.SPEAKER_BOX -> floatArrayOf(0.24f, 0.26f, 0.38f) to floatArrayOf(0.98f, 0.36f, 0.54f)
            PlatformStyle.TV_STAND -> floatArrayOf(0.84f, 0.52f, 0.96f) to floatArrayOf(0.45f, 0.25f, 0.62f)
            PlatformStyle.KITCHEN_COUNTER -> floatArrayOf(0.50f, 0.76f, 0.92f) to floatArrayOf(0.96f, 0.96f, 0.98f)
            PlatformStyle.ARMCHAIR -> floatArrayOf(0.98f, 0.48f, 0.42f) to floatArrayOf(0.76f, 0.30f, 0.28f)
            PlatformStyle.MINI_FRIDGE -> floatArrayOf(0.74f, 0.94f, 0.90f) to floatArrayOf(0.88f, 0.92f, 0.96f)
            PlatformStyle.MICROWAVE_STAND -> floatArrayOf(0.96f, 0.82f, 0.38f) to floatArrayOf(0.46f, 0.32f, 0.22f)
            PlatformStyle.WASHING_MACHINE -> floatArrayOf(0.92f, 0.95f, 0.98f) to floatArrayOf(0.32f, 0.68f, 0.95f)
            PlatformStyle.BAR_STOOL -> floatArrayOf(0.96f, 0.54f, 0.72f) to floatArrayOf(0.78f, 0.82f, 0.88f)
            PlatformStyle.NIGHTSTAND_LAMP -> floatArrayOf(0.72f, 0.56f, 0.44f) to floatArrayOf(0.48f, 0.35f, 0.26f)
        }

        platforms.add(
            Platform3D(
                id = nextPlatformId++,
                xCenter = prev.xCenter + dx,
                yTop = targetY,
                width = width,
                depth = depth,
                style = style,
                primaryRgb = primary,
                accentRgb = accent,
                isGoldBonus = isGold,
                requiresDoubleFlip = isDoubleFlipGap
            )
        )
    }

    fun onTapInput(): Boolean {
        synchronized(lock) {
            return when (phase) {
                GamePhase.AIMING -> {
                    if (controlMode == ControlMode.CLASSIC_DOUBLE_TAP) {
                        launchClassicTap()
                    } else {
                        launchWithPower(powerLevel)
                    }
                    true
                }
                GamePhase.FLYING -> {
                    if (airFlipAvailable) {
                        triggerAirFlip()
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
    }

    private fun launchClassicTap() {
        vx = CLASSIC_VX
        vy = CLASSIC_VY
        val flatFlightTime = (2f * vy) / -GRAVITY
        angularVelDeg = 360f / flatFlightTime

        phase = GamePhase.FLYING
        airFlipAvailable = true
        scaleX = 0.84f
        scaleY = 1.24f
        scaleZ = 0.84f

        spawnBurst(objX, objY + 0.1f, 0f, count = 8, speed = 2.4f, color = floatArrayOf(1f, 1f, 1f))
        eventListener?.onLaunch(0.55f)
    }

    private fun launchWithPower(power: Float) {
        val clamped = power.coerceIn(POWER_MIN, POWER_MAX)
        vx = 2.8f + clamped * 4.4f
        vy = 6.2f + clamped * 7.8f

        val flatFlightTime = (2f * vy) / -GRAVITY
        val targetTurns = if (clamped >= 0.72f) 2f else 1f
        angularVelDeg = (targetTurns * 360f) / flatFlightTime

        phase = GamePhase.FLYING
        airFlipAvailable = true
        scaleX = 0.84f
        scaleY = 1.24f
        scaleZ = 0.84f

        spawnBurst(objX, objY + 0.1f, 0f, count = 8, speed = 2.4f, color = floatArrayOf(1f, 1f, 1f))
        eventListener?.onLaunch(clamped)
    }

    private fun triggerAirFlip() {
        airFlipAvailable = false
        if (controlMode == ControlMode.CLASSIC_DOUBLE_TAP) {
            vy = max(vy + 4.4f, 7.6f)
            vx = AIR_FLIP_VX
            val targetPlat = platforms.getOrNull(currentPlatformIndex + 1)
            val targetY = targetPlat?.yTop ?: objY
            val dy = objY - targetY
            val a = 0.5f * -GRAVITY // +11.0
            val disc = max(0.01f, vy * vy + 4f * a * dy)
            val remTime = max(0.32f, (vy + sqrt(disc)) / -GRAVITY)
            val targetDeg = if (flipAngleDeg < 420f) 720f else ((flipAngleDeg / 360f).toInt() + 2) * 360f
            angularVelDeg = ((targetDeg - flipAngleDeg) / remTime).coerceIn(260f, 640f)
        } else {
            vy = max(vy + 3.6f, 6.4f)
            vx = max(vx * 0.94f, 2.6f)
            val remTime = max(0.35f, (vy + sqrt(max(0f, vy * vy))) / -GRAVITY)
            val remainingToNextUpright = 360f - (flipAngleDeg % 360f)
            angularVelDeg = (remainingToNextUpright + 360f) / remTime
        }

        scaleX = 0.82f
        scaleY = 1.28f
        scaleZ = 0.82f

        spawnBurst(objX, objY + 0.5f, 0f, count = 10, speed = 3.2f, color = floatArrayOf(0.28f, 0.95f, 0.82f))
        eventListener?.onAirFlip()
    }

    fun step(rawDt: Float) {
        synchronized(lock) {
            val dt = rawDt.coerceIn(0.001f, 0.033f)

            if (screenShake > 0f) {
                screenShake = max(0f, screenShake - dt * 2.8f)
            }
            if (feedbackTimer > 0f) {
                feedbackTimer -= dt
                if (feedbackTimer <= 0f) {
                    lastFeedback = null
                }
            }

            scaleX += (1f - scaleX) * min(1f, dt * 11f)
            scaleY += (1f - scaleY) * min(1f, dt * 11f)
            scaleZ += (1f - scaleZ) * min(1f, dt * 11f)

            val pIter = particles.iterator()
            while (pIter.hasNext()) {
                val p = pIter.next()
                p.life += dt
                if (p.life >= p.maxLife) {
                    pIter.remove()
                } else {
                    p.vy += GRAVITY * 0.55f * dt
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    p.z += p.vz * dt
                    p.rot += p.rotSpeed * dt
                }
            }

            when (phase) {
                GamePhase.TITLE -> {
                    oscillatorClock += dt * 1.45f
                    yawDeg = (yawDeg + dt * 36f) % 360f
                    updateAimCamera(dt)
                }
                GamePhase.AIMING -> {
                    if (controlMode == ControlMode.POWER_BAR) {
                        val speedMultiplier = 1.45f + (score.coerceAtMost(25) * 0.032f)
                        oscillatorClock += dt * speedMultiplier
                        val wave = 0.5f - 0.5f * cos(oscillatorClock * PI.toFloat())
                        powerLevel = POWER_MIN + wave * (POWER_MAX - POWER_MIN)

                        val targetStretchY = 0.90f + powerLevel * 0.24f
                        val targetSquashXZ = 1.06f - powerLevel * 0.10f
                        scaleY += (targetStretchY - scaleY) * min(1f, dt * 15f)
                        scaleX += (targetSquashXZ - scaleX) * min(1f, dt * 15f)
                        scaleZ += (targetSquashXZ - scaleZ) * min(1f, dt * 15f)
                    }
                    yawDeg += (0f - yawDeg) * min(1f, dt * 10f)
                    updateAimCamera(dt)
                }
                GamePhase.FLYING -> {
                    val prevY = objY
                    vy += GRAVITY * dt
                    objX += vx * dt
                    objY += vy * dt
                    flipAngleDeg += angularVelDeg * dt

                    // Smooth follow camera during flight
                    camX += (objX - camX) * min(1f, dt * 4.8f)
                    camY += (max(-0.4f, objY * 0.38f + 0.2f) - camY) * min(1f, dt * 4.0f)

                    checkPlatformCollision(prevY)

                    if (phase == GamePhase.FLYING && objY <= FLOOR_Y) {
                        objY = FLOOR_Y
                        enterCrashing(reason = "Missed the platform!")
                    }
                }
                GamePhase.WOBBLING -> {
                    wobbleTimer += dt
                    val progress = (wobbleTimer / 0.42f).coerceIn(0f, 1f)
                    val damped = sin(progress * PI.toFloat() * 3.5f) * (1f - progress) * wobbleStartAngle
                    flipAngleDeg = damped
                    updateAimCamera(dt)
                    if (progress >= 1f) {
                        flipAngleDeg = 0f
                        phase = GamePhase.AIMING
                        oscillatorClock = 0.15f
                        updateTutorialSweetZone()
                    }
                }
                GamePhase.CRASHING -> {
                    crashTimer += dt
                    vy += GRAVITY * dt
                    objX += vx * dt
                    objY += vy * dt
                    flipAngleDeg += angularVelDeg * dt
                    yawDeg += yawVelDeg * dt
                    rollDeg += rollVelDeg * dt

                    if (objY <= FLOOR_Y + 0.25f) {
                        objY = FLOOR_Y + 0.25f
                        if (floorBounces < 2 && abs(vy) > 1.8f) {
                            vy = abs(vy) * 0.42f
                            vx *= 0.55f
                            angularVelDeg *= 0.5f
                            rollVelDeg *= 0.5f
                            floorBounces++
                            screenShake = 0.25f
                            spawnBurst(objX, FLOOR_Y + 0.1f, 0f, count = 8, speed = 2.8f, color = floatArrayOf(1f, 0.7f, 0.4f))
                        } else {
                            vy = 0f
                            vx *= (1f - min(1f, dt * 5f))
                            angularVelDeg *= (1f - min(1f, dt * 5f))
                            yawVelDeg *= (1f - min(1f, dt * 5f))
                            rollVelDeg *= (1f - min(1f, dt * 5f))
                        }
                    }

                    if (crashTimer >= 0.95f) {
                        phase = GamePhase.GAME_OVER
                        eventListener?.onCrash(score, maxRunStreak)
                    }
                }
                GamePhase.GAME_OVER -> {
                    camX += (objX - camX) * min(1f, dt * 2.5f)
                }
            }
        }
    }

    private fun updateAimCamera(dt: Float) {
        val current = platforms.getOrNull(currentPlatformIndex)
        val next = platforms.getOrNull(currentPlatformIndex + 1)
        val targetCamX = current?.xCenter ?: objX
        val targetCamY = if (current != null && next != null) {
            ((current.yTop + next.yTop) * 0.5f) + 0.2f
        } else {
            objY + 0.2f
        }
        camX += (targetCamX - camX) * min(1f, dt * 4.5f)
        camY += (targetCamY - camY) * min(1f, dt * 4.5f)
    }

    private fun checkPlatformCollision(prevY: Float) {
        for (i in platforms.indices) {
            val plat = platforms[i]
            val halfW = plat.width * 0.5f
            val landingMargin = 0.18f
            val withinTopX = objX >= (plat.xCenter - halfW - landingMargin) &&
                objX <= (plat.xCenter + halfW + landingMargin)

            if (vy <= 0f && prevY >= plat.yTop - 0.15f && objY <= plat.yTop && withinTopX) {
                val normalizedAngle = normalizeAngleDeg(flipAngleDeg)
                val absAngle = abs(normalizedAngle)

                if (i <= currentPlatformIndex) {
                    if (absAngle <= SAVE_ANGLE_DEG) {
                        objY = plat.yTop
                        vx = 0f
                        vy = 0f
                        flipAngleDeg = 0f
                        scaleX = 1.24f
                        scaleY = 0.72f
                        scaleZ = 1.24f
                        phase = GamePhase.AIMING
                        oscillatorClock = 0.15f
                        showFeedback("TRY AGAIN!", "+0", isPerfect = false)
                        eventListener?.onLand(isPerfect = false, streak = streak, score = score)
                    } else {
                        objY = plat.yTop
                        enterCrashing(reason = "Landed sideways!")
                    }
                    return
                }

                objY = plat.yTop
                when {
                    absAngle <= PERFECT_ANGLE_DEG -> {
                        currentPlatformIndex = i
                        jumpsCompleted++
                        streak++
                        perfectCount++
                        if (streak > maxRunStreak) maxRunStreak = streak
                        if (streak > bestStreak) bestStreak = streak

                        val bonusMultiplier = if (plat.isGoldBonus) 2 else 1
                        val earned = (if (streak >= 2) 2 else 1) * bonusMultiplier
                        score += earned
                        if (score > bestScore) {
                            bestScore = score
                            isNewBest = true
                        }

                        vx = 0f
                        vy = 0f
                        flipAngleDeg = 0f
                        scaleX = 1.32f
                        scaleY = 0.66f
                        scaleZ = 1.32f
                        phase = GamePhase.AIMING
                        oscillatorClock = 0.15f
                        ensurePlatformsAhead()
                        updateTutorialSweetZone()

                        val title = when {
                            plat.isGoldBonus -> "GOLD PEDESTAL!"
                            streak >= 3 -> "STREAK x$streak!"
                            else -> "PERFECT!"
                        }
                        showFeedback(title, "+$earned", isPerfect = true, isBonus = plat.isGoldBonus)
                        spawnConfetti(objX, objY + 0.5f)
                        eventListener?.onLand(isPerfect = true, streak = streak, score = score)
                    }
                    absAngle <= SAVE_ANGLE_DEG -> {
                        currentPlatformIndex = i
                        jumpsCompleted++
                        streak = 0
                        val earned = if (plat.isGoldBonus) 2 else 1
                        score += earned
                        if (score > bestScore) {
                            bestScore = score
                            isNewBest = true
                        }

                        vx = 0f
                        vy = 0f
                        wobbleTimer = 0f
                        wobbleStartAngle = normalizedAngle
                        scaleX = 1.18f
                        scaleY = 0.78f
                        scaleZ = 1.18f
                        phase = GamePhase.WOBBLING
                        ensurePlatformsAhead()
                        updateTutorialSweetZone()

                        showFeedback("CLOSE SAVE!", "+$earned", isPerfect = false, isBonus = plat.isGoldBonus)
                        spawnBurst(objX, objY + 0.1f, 0f, count = 10, speed = 2.6f, color = floatArrayOf(1f, 0.85f, 0.25f))
                        eventListener?.onWobbleSave(score = score)
                    }
                    else -> {
                        vy = 3.5f
                        vx *= 0.4f
                        enterCrashing(reason = "Bad landing angle!")
                    }
                }
                return
            }

            val withinSideX = abs(objX - plat.xCenter) <= (halfW + 0.16f)
            if (objY < plat.yTop - 0.15f && objY > plat.yTop - 1.1f && withinSideX) {
                vx = -abs(vx) * 0.45f
                vy = max(vy * 0.3f, 1.5f)
                enterCrashing(reason = "Bonked the edge!")
                return
            }
        }
    }

    private fun enterCrashing(reason: String) {
        if (phase == GamePhase.CRASHING || phase == GamePhase.GAME_OVER) return
        phase = GamePhase.CRASHING
        failReason = reason
        crashTimer = 0f
        floorBounces = 0
        streak = 0
        screenShake = 0.45f
        angularVelDeg = if (angularVelDeg >= 0f) 420f else -420f
        yawVelDeg = (random.nextFloat() * 2f - 1f) * 320f
        rollVelDeg = (random.nextFloat() * 2f - 1f) * 380f
        spawnBurst(objX, objY + 0.2f, 0f, count = 14, speed = 3.6f, color = floatArrayOf(1f, 0.35f, 0.42f))
    }

    private fun ensurePlatformsAhead() {
        while (platforms.size - 1 - currentPlatformIndex < 9) {
            generateNextPlatform()
        }
        while (currentPlatformIndex > 3) {
            platforms.removeAt(0)
            currentPlatformIndex--
        }
    }

    private fun normalizeAngleDeg(deg: Float): Float {
        var a = deg % 360f
        if (a > 180f) a -= 360f
        if (a < -180f) a += 360f
        return a
    }

    private fun showFeedback(title: String, pointsText: String, isPerfect: Boolean, isBonus: Boolean = false) {
        lastFeedback = LandingFeedback(
            id = ++feedbackCounter,
            title = title,
            pointsText = pointsText,
            isPerfect = isPerfect,
            isBonus = isBonus
        )
        feedbackTimer = 1.25f
    }

    private fun spawnConfetti(x: Float, y: Float) {
        val palette = listOf(
            floatArrayOf(1.00f, 0.35f, 0.45f),
            floatArrayOf(0.18f, 0.90f, 0.72f),
            floatArrayOf(1.00f, 0.80f, 0.22f),
            floatArrayOf(0.55f, 0.42f, 1.00f),
            floatArrayOf(0.32f, 0.82f, 1.00f)
        )
        repeat(22) {
            val angle = random.nextFloat() * PI.toFloat() * 2f
            val speed = 2.2f + random.nextFloat() * 4.5f
            particles.add(
                Particle3D(
                    x = x,
                    y = y,
                    z = (random.nextFloat() - 0.5f) * 0.4f,
                    vx = cos(angle) * speed * 0.85f,
                    vy = abs(sin(angle)) * speed + 2.5f,
                    vz = (random.nextFloat() - 0.5f) * 3.0f,
                    rot = random.nextFloat() * 360f,
                    rotSpeed = (random.nextFloat() - 0.5f) * 540f,
                    size = 0.08f + random.nextFloat() * 0.08f,
                    life = 0f,
                    maxLife = 0.8f + random.nextFloat() * 0.45f,
                    rgb = palette[random.nextInt(palette.size)]
                )
            )
        }
    }

    private fun spawnBurst(x: Float, y: Float, z: Float, count: Int, speed: Float, color: FloatArray) {
        repeat(count) {
            val a = random.nextFloat() * PI.toFloat() * 2f
            val sp = speed * (0.4f + random.nextFloat() * 0.8f)
            particles.add(
                Particle3D(
                    x = x,
                    y = y,
                    z = z + (random.nextFloat() - 0.5f) * 0.25f,
                    vx = cos(a) * sp,
                    vy = abs(sin(a)) * sp + 1.0f,
                    vz = (random.nextFloat() - 0.5f) * sp,
                    rot = random.nextFloat() * 360f,
                    rotSpeed = (random.nextFloat() - 0.5f) * 360f,
                    size = 0.06f + random.nextFloat() * 0.06f,
                    life = 0f,
                    maxLife = 0.45f + random.nextFloat() * 0.25f,
                    rgb = color
                )
            )
        }
    }

    private fun computeNearestSurfaceY(): Float {
        var bestY = FLOOR_Y
        for (plat in platforms) {
            val halfW = plat.width * 0.5f + 0.15f
            if (objX >= plat.xCenter - halfW && objX <= plat.xCenter + halfW && objY >= plat.yTop - 0.1f) {
                if (plat.yTop > bestY) {
                    bestY = plat.yTop
                }
            }
        }
        return bestY
    }

    fun getRenderSnapshot(): GameRenderSnapshot = synchronized(lock) {
        GameRenderSnapshot(
            phase = phase,
            objX = objX,
            objY = objY,
            objZ = objZ,
            flipAngleDeg = flipAngleDeg,
            yawDeg = yawDeg,
            rollDeg = rollDeg,
            scaleX = scaleX,
            scaleY = scaleY,
            scaleZ = scaleZ,
            powerLevel = powerLevel,
            showPowerBarOnObject = (controlMode == ControlMode.POWER_BAR && phase == GamePhase.AIMING),
            camX = camX,
            camY = camY,
            screenShake = screenShake,
            nearestSurfaceY = computeNearestSurfaceY(),
            platforms = platforms.toList(),
            currentPlatformIndex = currentPlatformIndex,
            particles = particles.map { it.copy() },
            skin = selectedSkin,
            theme = selectedTheme
        )
    }

    fun getUiState(): GameUiState = synchronized(lock) {
        val nextGapDouble = platforms.getOrNull(currentPlatformIndex + 1)?.requiresDoubleFlip ?: false
        GameUiState(
            phase = phase,
            score = score,
            bestScore = bestScore,
            streak = streak,
            maxRunStreak = maxRunStreak,
            bestStreak = bestStreak,
            perfectCount = perfectCount,
            jumpsCompleted = jumpsCompleted,
            powerLevel = powerLevel,
            tutorialSweetZone = tutorialSweetZone,
            airFlipAvailable = airFlipAvailable,
            nextGapNeedsDoubleFlip = nextGapDouble,
            lastFeedback = lastFeedback,
            isNewBest = isNewBest,
            failReason = failReason,
            selectedSkin = selectedSkin,
            selectedTheme = selectedTheme,
            controlMode = controlMode,
            soundEnabled = soundEnabled,
            hapticsEnabled = hapticsEnabled
        )
    }
}
