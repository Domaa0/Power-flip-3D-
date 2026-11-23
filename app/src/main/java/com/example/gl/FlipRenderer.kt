package com.example.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.FlipShape
import com.example.game.GameEngine
import com.example.game.GameRenderSnapshot
import com.example.game.Platform3D
import com.example.game.PlatformStyle
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max
import kotlin.math.sin

class FlipRenderer(
    private val engine: GameEngine,
    private val onFrameTick: () -> Unit
) : GLSurfaceView.Renderer {

    private var worldProgram = 0
    private var bgProgram = 0
    private var shadowProgram = 0

    // World shader handles
    private var uMvpMatrix = 0
    private var uModelMatrix = 0
    private var uBaseColor = 0
    private var uShowPowerBar = 0
    private var uPowerFill = 0
    private var uSpecStrength = 0
    private var uAlpha = 0
    private var aPosition = 0
    private var aNormal = 0

    // Background shader handles
    private var bgAPosition = 0
    private var bgUColorTop = 0
    private var bgUColorBottom = 0

    // Shadow shader handles
    private var shUMvpMatrix = 0
    private var shUAlpha = 0
    private var shAPosition = 0

    // Environment & furniture meshes
    private lateinit var boxMesh: MeshGeometry
    private lateinit var cylMesh: MeshGeometry
    private lateinit var floorMesh: MeshGeometry

    // Penis skin meshes
    private lateinit var heroLobes: MeshGeometry
    private lateinit var heroShaft: MeshGeometry
    private lateinit var heroCollar: MeshGeometry
    private lateinit var heroCap: MeshGeometry
    private lateinit var heroEyeWhites: MeshGeometry
    private lateinit var heroEyePupils: MeshGeometry

    // Pussy skin meshes
    private lateinit var pussyOuterBody: MeshGeometry
    private lateinit var pussyInnerPetals: MeshGeometry
    private lateinit var pussyCrest: MeshGeometry
    private lateinit var pussyEyeWhites: MeshGeometry
    private lateinit var pussyEyePupils: MeshGeometry

    // Classic bottle meshes
    private lateinit var waterBody: MeshGeometry
    private lateinit var waterLabel: MeshGeometry
    private lateinit var waterCap: MeshGeometry
    private lateinit var sodaBody: MeshGeometry
    private lateinit var sodaLabel: MeshGeometry
    private lateinit var sodaCap: MeshGeometry
    private lateinit var trophyBody: MeshGeometry
    private lateinit var trophyFoil: MeshGeometry

    private lateinit var bgQuadBuffer: FloatBuffer

    // Matrices & viewport aspect
    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private var viewportAspect = 0.56f

    private var lastTimeNs = 0L
    private var totalTime = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        worldProgram = buildProgram(WORLD_VERT_SHADER, WORLD_FRAG_SHADER)
        uMvpMatrix = GLES20.glGetUniformLocation(worldProgram, "uMvpMatrix")
        uModelMatrix = GLES20.glGetUniformLocation(worldProgram, "uModelMatrix")
        uBaseColor = GLES20.glGetUniformLocation(worldProgram, "uBaseColor")
        uShowPowerBar = GLES20.glGetUniformLocation(worldProgram, "uShowPowerBar")
        uPowerFill = GLES20.glGetUniformLocation(worldProgram, "uPowerFill")
        uSpecStrength = GLES20.glGetUniformLocation(worldProgram, "uSpecStrength")
        uAlpha = GLES20.glGetUniformLocation(worldProgram, "uAlpha")
        aPosition = GLES20.glGetAttribLocation(worldProgram, "aPosition")
        aNormal = GLES20.glGetAttribLocation(worldProgram, "aNormal")

        bgProgram = buildProgram(BG_VERT_SHADER, BG_FRAG_SHADER)
        bgAPosition = GLES20.glGetAttribLocation(bgProgram, "aPosition")
        bgUColorTop = GLES20.glGetUniformLocation(bgProgram, "uColorTop")
        bgUColorBottom = GLES20.glGetUniformLocation(bgProgram, "uColorBottom")

        shadowProgram = buildProgram(SHADOW_VERT_SHADER, SHADOW_FRAG_SHADER)
        shUMvpMatrix = GLES20.glGetUniformLocation(shadowProgram, "uMvpMatrix")
        shUAlpha = GLES20.glGetUniformLocation(shadowProgram, "uAlpha")
        shAPosition = GLES20.glGetAttribLocation(shadowProgram, "aPosition")

        boxMesh = MeshBuilder.createUnitBox()
        cylMesh = MeshBuilder.createUnitCylinder(24)
        floorMesh = MeshBuilder.createFloorQuad()

        heroLobes = MeshBuilder.createHeroBaseLobes()
        heroShaft = MeshBuilder.createHeroShaft()
        heroCollar = MeshBuilder.createHeroCollar()
        heroCap = MeshBuilder.createHeroCap()
        heroEyeWhites = MeshBuilder.createHeroEyeWhites()
        heroEyePupils = MeshBuilder.createHeroEyePupils()

        pussyOuterBody = MeshBuilder.createPussyOuterBody()
        pussyInnerPetals = MeshBuilder.createPussyInnerPetals()
        pussyCrest = MeshBuilder.createPussyCrest()
        pussyEyeWhites = MeshBuilder.createPussyEyeWhites()
        pussyEyePupils = MeshBuilder.createPussyEyePupils()

        waterBody = MeshBuilder.createWaterBottleBody()
        waterLabel = MeshBuilder.createWaterBottleLabel()
        waterCap = MeshBuilder.createWaterBottleCap()
        sodaBody = MeshBuilder.createSodaBottleBody()
        sodaLabel = MeshBuilder.createSodaBottleLabel()
        sodaCap = MeshBuilder.createSodaBottleCap()
        trophyBody = MeshBuilder.createTrophyBottleBody()
        trophyFoil = MeshBuilder.createTrophyBottleFoil()

        val bgCoords = floatArrayOf(
            -1f, -1f,
             1f, -1f,
             1f,  1f,
            -1f, -1f,
             1f,  1f,
            -1f,  1f
        )
        bgQuadBuffer = ByteBuffer.allocateDirect(bgCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(bgCoords)
                position(0)
            }

        lastTimeNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewportAspect = width.toFloat() / max(1, height).toFloat()
        Matrix.perspectiveM(projMatrix, 0, 55f, viewportAspect, 0.5f, 120f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastTimeNs) / 1_000_000_000.0f).coerceIn(0.001f, 0.033f)
        lastTimeNs = now
        totalTime += dt

        engine.step(dt)
        onFrameTick()

        val snap = engine.getRenderSnapshot()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        drawBackdrop(snap)
        setupCamera(snap)
        drawStudioFloor(snap)
        drawWallDecor(snap)

        for (i in snap.platforms.indices) {
            val plat = snap.platforms[i]
            val isNextTarget = (i == snap.currentPlatformIndex + 1)
            drawPlatform(plat, isNextTarget)
        }

        drawContactShadow(snap)
        drawHeroObject(snap)
        drawParticles(snap)
    }

    private fun drawBackdrop(snap: GameRenderSnapshot) {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glUseProgram(bgProgram)
        val top = snap.theme.bgTopRgb
        val bot = snap.theme.bgBottomRgb
        GLES20.glUniform3f(bgUColorTop, top[0], top[1], top[2])
        GLES20.glUniform3f(bgUColorBottom, bot[0], bot[1], bot[2])
        GLES20.glEnableVertexAttribArray(bgAPosition)
        bgQuadBuffer.position(0)
        GLES20.glVertexAttribPointer(bgAPosition, 2, GLES20.GL_FLOAT, false, 8, bgQuadBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        GLES20.glDisableVertexAttribArray(bgAPosition)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
    }

    private fun setupCamera(snap: GameRenderSnapshot) {
        val shakeX = if (snap.screenShake > 0f) sin(totalTime * 55f) * snap.screenShake * 0.25f else 0f
        val shakeY = if (snap.screenShake > 0f) sin(totalTime * 71f + 1.1f) * snap.screenShake * 0.25f else 0f

        // Wide pulled-back 3/4 room view keeping at least 5 furniture pieces clearly visible ahead
        val isPortrait = viewportAspect < 0.75f
        val camDistZ = if (isPortrait) 24.5f else 17.5f
        val eyeX = snap.camX - (if (isPortrait) 1.5f else 0.6f) + shakeX
        val targetX = snap.camX + (if (isPortrait) 5.6f else 4.6f) + shakeX

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeX, snap.camY + 5.8f + shakeY, camDistZ,
            targetX, snap.camY - 0.2f + shakeY, 0.0f,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
    }

    private fun drawStudioFloor(snap: GameRenderSnapshot) {
        GLES20.glUseProgram(worldProgram)
        val floorColor = snap.theme.floorRgb
        val floorStart = ((snap.camX / 4f).toInt() - 6) * 4f
        for (tileIdx in 0..12) {
            val tx = floorStart + tileIdx * 4f
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, tx, GameEngine.FLOOR_Y - 0.02f, -1.5f)
            Matrix.scaleM(modelMatrix, 0, 3.94f, 1f, 18f)
            val shade = if (tileIdx % 2 == 0) 1.0f else 0.93f
            drawMesh(
                floorMesh,
                floatArrayOf(floorColor[0] * shade, floorColor[1] * shade, floorColor[2] * shade),
                showPower = false,
                powerFill = 0f,
                spec = 0.10f
            )
        }
    }

    private fun drawWallDecor(snap: GameRenderSnapshot) {
        GLES20.glUseProgram(worldProgram)
        val accent = snap.theme.wallAccentRgb
        val startIdx = ((snap.camX / 8f).toInt() - 3)
        for (i in startIdx..(startIdx + 6)) {
            val wx = i * 8f
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, wx, 2.5f + (i and 1) * 0.75f, -6.5f)
            Matrix.scaleM(modelMatrix, 0, 2.8f, 1.65f, 0.22f)
            drawMesh(boxMesh, accent, showPower = false, powerFill = 0f, spec = 0.15f, alpha = 0.60f)
        }
    }

    private fun drawPlatform(plat: Platform3D, isNextTarget: Boolean) {
        GLES20.glUseProgram(worldProgram)
        val floorY = GameEngine.FLOOR_Y
        val legHeight = max(0.3f, plat.yTop - floorY)

        when (plat.style) {
            PlatformStyle.SIDE_TABLE -> {
                val topThick = 0.22f
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - topThick * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width, topThick, plat.depth)
                drawMesh(boxMesh, plat.primaryRgb, showPower = false, powerFill = 0f, spec = 0.25f)

                val lx = plat.width * 0.40f
                val lz = plat.depth * 0.36f
                val legBodyH = max(0.2f, legHeight - topThick)
                val legCenterY = plat.yTop - topThick - legBodyH * 0.5f
                for (sx in floatArrayOf(-1f, 1f)) {
                    for (sz in floatArrayOf(-1f, 1f)) {
                        Matrix.setIdentityM(modelMatrix, 0)
                        Matrix.translateM(modelMatrix, 0, plat.xCenter + sx * lx, legCenterY, sz * lz)
                        Matrix.scaleM(modelMatrix, 0, 0.08f, legBodyH, 0.08f)
                        drawMesh(cylMesh, plat.accentRgb, showPower = false, powerFill = 0f, spec = 0.15f)
                    }
                }
            }
            PlatformStyle.BOOK_STACK -> {
                val bookCount = 4
                val bookH = minOf(0.32f, legHeight / bookCount)
                for (b in 0 until bookCount) {
                    val col = if (b % 2 == 0) plat.primaryRgb else plat.accentRgb
                    val wScale = if (b == 0) 1f else 0.94f - (b % 2) * 0.05f
                    val xShift = if (b % 2 == 0) 0f else 0.05f
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, plat.xCenter + xShift, plat.yTop - bookH * (b + 0.5f), 0f)
                    Matrix.scaleM(modelMatrix, 0, plat.width * wScale, bookH * 0.94f, plat.depth * 0.92f)
                    drawMesh(boxMesh, col, showPower = false, powerFill = 0f, spec = 0.2f)
                }
                val remH = legHeight - bookCount * bookH
                if (remH > 0.05f) {
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, plat.xCenter, floorY + remH * 0.5f, 0f)
                    Matrix.scaleM(modelMatrix, 0, plat.width * 0.72f, remH, plat.depth * 0.72f)
                    drawMesh(boxMesh, floatArrayOf(0.86f, 0.80f, 0.72f), showPower = false, powerFill = 0f, spec = 0.1f)
                }
            }
            PlatformStyle.OTTOMAN -> {
                val cushionH = 0.52f
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - cushionH * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width, cushionH, plat.depth)
                drawMesh(boxMesh, plat.primaryRgb, showPower = false, powerFill = 0f, spec = 0.18f)

                val bodyH = max(0.2f, legHeight - cushionH)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - cushionH - bodyH * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width * 0.86f, bodyH, plat.depth * 0.86f)
                drawMesh(boxMesh, plat.accentRgb, showPower = false, powerFill = 0f, spec = 0.12f)
            }
            PlatformStyle.SPEAKER_BOX -> {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - legHeight * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width, legHeight, plat.depth)
                drawMesh(boxMesh, plat.primaryRgb, showPower = false, powerFill = 0f, spec = 0.30f)

                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - minOf(0.75f, legHeight * 0.45f), plat.depth * 0.5f + 0.02f)
                Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width * 0.32f, 0.08f, plat.width * 0.32f)
                drawMesh(cylMesh, plat.accentRgb, showPower = false, powerFill = 0f, spec = 0.55f)
            }
            PlatformStyle.TV_STAND -> {
                val topH = 0.24f
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - topH * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width, topH, plat.depth)
                drawMesh(boxMesh, plat.primaryRgb, showPower = false, powerFill = 0f, spec = 0.32f)

                val bodyH = max(0.2f, legHeight - topH)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - topH - bodyH * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width * 0.88f, bodyH, plat.depth * 0.84f)
                drawMesh(boxMesh, plat.accentRgb, showPower = false, powerFill = 0f, spec = 0.16f)

                // Flat-screen TV panel resting toward the back edge
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop + 0.55f, -plat.depth * 0.38f)
                Matrix.scaleM(modelMatrix, 0, plat.width * 0.82f, 0.92f, 0.09f)
                drawMesh(boxMesh, floatArrayOf(0.14f, 0.16f, 0.24f), showPower = false, powerFill = 0f, spec = 0.75f)
            }
            PlatformStyle.KITCHEN_COUNTER -> {
                val slabH = 0.20f
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - slabH * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width, slabH, plat.depth)
                drawMesh(boxMesh, floatArrayOf(0.96f, 0.96f, 0.98f), showPower = false, powerFill = 0f, spec = 0.55f)

                val cabH = max(0.2f, legHeight - slabH)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - slabH - cabH * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width * 0.94f, cabH, plat.depth * 0.90f)
                drawMesh(boxMesh, plat.primaryRgb, showPower = false, powerFill = 0f, spec = 0.15f)
            }
            PlatformStyle.ARMCHAIR -> {
                val seatH = 0.44f
                // Seat cushion
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - seatH * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width, seatH, plat.depth)
                drawMesh(boxMesh, plat.primaryRgb, showPower = false, powerFill = 0f, spec = 0.20f)

                // Backrest behind landing surface
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop + 0.42f, -plat.depth * 0.38f)
                Matrix.scaleM(modelMatrix, 0, plat.width * 0.96f, 0.92f, 0.28f)
                drawMesh(boxMesh, plat.accentRgb, showPower = false, powerFill = 0f, spec = 0.18f)

                // Base legs
                val baseH = max(0.2f, legHeight - seatH)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - seatH - baseH * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width * 0.85f, baseH, plat.depth * 0.82f)
                drawMesh(boxMesh, floatArrayOf(0.45f, 0.30f, 0.20f), showPower = false, powerFill = 0f, spec = 0.15f)
            }
            PlatformStyle.MINI_FRIDGE -> {
                // Tall crisp fridge body
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - legHeight * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width, legHeight, plat.depth)
                drawMesh(boxMesh, plat.primaryRgb, showPower = false, powerFill = 0f, spec = 0.50f)

                // Door seam & silver handle on front +Z
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter - plat.width * 0.26f, plat.yTop - minOf(0.70f, legHeight * 0.35f), plat.depth * 0.5f + 0.04f)
                Matrix.scaleM(modelMatrix, 0, 0.09f, minOf(0.85f, legHeight * 0.42f), 0.07f)
                drawMesh(boxMesh, plat.accentRgb, showPower = false, powerFill = 0f, spec = 0.85f)
            }
            PlatformStyle.MICROWAVE_STAND -> {
                val mwH = minOf(0.95f, legHeight * 0.42f)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - mwH * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width, mwH, plat.depth)
                drawMesh(boxMesh, plat.primaryRgb, showPower = false, powerFill = 0f, spec = 0.45f)

                // Dark microwave glass window
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter - plat.width * 0.10f, plat.yTop - mwH * 0.5f, plat.depth * 0.5f + 0.02f)
                Matrix.scaleM(modelMatrix, 0, plat.width * 0.56f, mwH * 0.62f, 0.04f)
                drawMesh(boxMesh, floatArrayOf(0.14f, 0.16f, 0.22f), showPower = false, powerFill = 0f, spec = 0.8f)

                // Supporting stand below
                val standH = max(0.2f, legHeight - mwH)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - mwH - standH * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width * 0.90f, standH, plat.depth * 0.88f)
                drawMesh(boxMesh, plat.accentRgb, showPower = false, powerFill = 0f, spec = 0.20f)
            }
            PlatformStyle.WASHING_MACHINE -> {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - legHeight * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width, legHeight, plat.depth)
                drawMesh(boxMesh, plat.primaryRgb, showPower = false, powerFill = 0f, spec = 0.52f)

                // Front round drum porthole
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - minOf(1.1f, legHeight * 0.48f), plat.depth * 0.5f + 0.03f)
                Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width * 0.34f, 0.09f, plat.width * 0.34f)
                drawMesh(cylMesh, plat.accentRgb, showPower = false, powerFill = 0f, spec = 0.78f)
            }
            PlatformStyle.BAR_STOOL -> {
                val seatH = 0.24f
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - seatH * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width * 0.52f, seatH, plat.depth * 0.52f)
                drawMesh(cylMesh, plat.primaryRgb, showPower = false, powerFill = 0f, spec = 0.45f)

                val stemH = max(0.2f, legHeight - seatH)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - seatH - stemH * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, 0.13f, stemH, 0.13f)
                drawMesh(cylMesh, plat.accentRgb, showPower = false, powerFill = 0f, spec = 0.75f)
            }
            PlatformStyle.NIGHTSTAND_LAMP -> {
                val topH = 0.20f
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - topH * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width, topH, plat.depth)
                drawMesh(boxMesh, plat.primaryRgb, showPower = false, powerFill = 0f, spec = 0.28f)

                val bodyH = max(0.2f, legHeight - topH)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - topH - bodyH * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width * 0.90f, bodyH, plat.depth * 0.88f)
                drawMesh(boxMesh, plat.accentRgb, showPower = false, powerFill = 0f, spec = 0.20f)

                // Cute glowing bedside lamp at back corner
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter + plat.width * 0.26f, plat.yTop + 0.34f, -plat.depth * 0.32f)
                Matrix.scaleM(modelMatrix, 0, 0.24f, 0.42f, 0.24f)
                drawMesh(cylMesh, floatArrayOf(1.00f, 0.88f, 0.44f), showPower = false, powerFill = 0f, spec = 0.4f)
            }
            PlatformStyle.TROPHY_PEDESTAL -> {
                val capH = 0.22f
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - capH * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width * 0.52f, capH, plat.depth * 0.52f)
                drawMesh(cylMesh, plat.primaryRgb, showPower = false, powerFill = 0f, spec = 0.85f)

                val colH = max(0.2f, legHeight - capH)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop - capH - colH * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, plat.width * 0.36f, colH, plat.depth * 0.36f)
                drawMesh(cylMesh, plat.accentRgb, showPower = false, powerFill = 0f, spec = 0.70f)
            }
        }

        // Pulsing landing ring on the next target furniture
        if (isNextTarget) {
            val pulse = 0.58f + 0.12f * sin(totalTime * 5.2f)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, plat.xCenter, plat.yTop + 0.015f, 0f)
            Matrix.scaleM(modelMatrix, 0, plat.width * 0.34f * pulse, 0.02f, plat.depth * 0.34f * pulse)
            val ringCol = if (plat.isGoldBonus) floatArrayOf(1f, 0.90f, 0.28f) else floatArrayOf(0.22f, 0.88f, 0.72f)
            drawMesh(cylMesh, ringCol, showPower = false, powerFill = 0f, spec = 0.55f, alpha = 0.85f)
        }
    }

    private fun drawContactShadow(snap: GameRenderSnapshot) {
        val heightAbove = max(0f, snap.objY - snap.nearestSurfaceY)
        val radiusScale = (1.15f - heightAbove * 0.14f).coerceIn(0.38f, 1.25f)
        val alpha = (0.36f - heightAbove * 0.055f).coerceIn(0.06f, 0.36f)

        GLES20.glUseProgram(shadowProgram)
        GLES20.glDepthMask(false)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, snap.objX, snap.nearestSurfaceY + 0.018f, snap.objZ)
        Matrix.scaleM(modelMatrix, 0, radiusScale * 0.95f, 1f, radiusScale * 0.85f)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES20.glUniformMatrix4fv(shUMvpMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(shUAlpha, alpha)

        GLES20.glEnableVertexAttribArray(shAPosition)
        floorMesh.buffer.position(0)
        GLES20.glVertexAttribPointer(shAPosition, 3, GLES20.GL_FLOAT, false, 24, floorMesh.buffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, floorMesh.vertexCount)
        GLES20.glDisableVertexAttribArray(shAPosition)
        GLES20.glDepthMask(true)
    }

    private fun drawHeroObject(snap: GameRenderSnapshot) {
        GLES20.glUseProgram(worldProgram)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, snap.objX, snap.objY, snap.objZ)

        // Pivot around center of mass
        Matrix.translateM(modelMatrix, 0, 0f, 0.78f, 0f)
        Matrix.rotateM(modelMatrix, 0, snap.yawDeg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, -snap.flipAngleDeg, 0f, 0f, 1f)
        Matrix.rotateM(modelMatrix, 0, snap.rollDeg, 1f, 0f, 0f)
        Matrix.translateM(modelMatrix, 0, 0f, -0.78f, 0f)

        // Bold readable scale in pulled-back 3D room view + squash/stretch
        val heroScale = 1.18f
        Matrix.scaleM(modelMatrix, 0, snap.scaleX * heroScale, snap.scaleY * heroScale, snap.scaleZ * heroScale)

        val showBar = snap.showPowerBarOnObject
        val fill = snap.powerLevel
        val skin = snap.skin

        when (skin.shape) {
            FlipShape.PENIS_SKIN -> {
                drawMesh(heroLobes, skin.bodyRgb, showPower = showBar, powerFill = fill, spec = 0.65f)
                drawMesh(heroShaft, skin.bodyRgb, showPower = showBar, powerFill = fill, spec = 0.65f)
                drawMesh(heroCollar, skin.accentRgb, showPower = showBar, powerFill = fill, spec = 0.72f)
                drawMesh(heroCap, skin.capRgb, showPower = showBar, powerFill = fill, spec = 0.78f)
                drawMesh(heroEyeWhites, floatArrayOf(0.98f, 0.98f, 1.0f), showPower = false, powerFill = 0f, spec = 0.4f)
                drawMesh(heroEyePupils, floatArrayOf(0.10f, 0.09f, 0.16f), showPower = false, powerFill = 0f, spec = 0.8f)
            }
            FlipShape.PUSSY_SKIN -> {
                drawMesh(pussyOuterBody, skin.bodyRgb, showPower = showBar, powerFill = fill, spec = 0.62f)
                drawMesh(pussyInnerPetals, skin.accentRgb, showPower = showBar, powerFill = fill, spec = 0.72f)
                drawMesh(pussyCrest, skin.capRgb, showPower = showBar, powerFill = fill, spec = 0.85f)
                drawMesh(pussyEyeWhites, floatArrayOf(0.98f, 0.98f, 1.0f), showPower = false, powerFill = 0f, spec = 0.4f)
                drawMesh(pussyEyePupils, floatArrayOf(0.10f, 0.09f, 0.16f), showPower = false, powerFill = 0f, spec = 0.8f)
            }
            FlipShape.WATER_BOTTLE -> {
                drawMesh(waterBody, skin.bodyRgb, showPower = showBar, powerFill = fill, spec = 0.88f, alpha = 0.90f)
                drawMesh(waterLabel, skin.accentRgb, showPower = showBar, powerFill = fill, spec = 0.45f)
                drawMesh(waterCap, skin.capRgb, showPower = showBar, powerFill = fill, spec = 0.75f)
            }
            FlipShape.SODA_BOTTLE -> {
                drawMesh(sodaBody, skin.bodyRgb, showPower = showBar, powerFill = fill, spec = 0.92f)
                drawMesh(sodaLabel, skin.accentRgb, showPower = showBar, powerFill = fill, spec = 0.50f)
                drawMesh(sodaCap, skin.capRgb, showPower = showBar, powerFill = fill, spec = 0.80f)
            }
            FlipShape.TROPHY_BOTTLE -> {
                drawMesh(trophyBody, skin.bodyRgb, showPower = showBar, powerFill = fill, spec = 0.85f)
                drawMesh(trophyFoil, skin.capRgb, showPower = showBar, powerFill = fill, spec = 0.95f)
            }
        }
    }

    private fun drawParticles(snap: GameRenderSnapshot) {
        if (snap.particles.isEmpty()) return
        GLES20.glUseProgram(worldProgram)
        for (p in snap.particles) {
            val lifeRatio = 1f - (p.life / p.maxLife).coerceIn(0f, 1f)
            val s = p.size * (0.4f + 0.6f * lifeRatio)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z)
            Matrix.rotateM(modelMatrix, 0, p.rot, 0.6f, 1f, 0.3f)
            Matrix.scaleM(modelMatrix, 0, s, s, s)
            drawMesh(boxMesh, p.rgb, showPower = false, powerFill = 0f, spec = 0.3f, alpha = lifeRatio)
        }
    }

    private fun drawMesh(
        mesh: MeshGeometry,
        colorRgb: FloatArray,
        showPower: Boolean,
        powerFill: Float,
        spec: Float,
        alpha: Float = 1f
    ) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelMatrix, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(uBaseColor, colorRgb[0], colorRgb[1], colorRgb[2])
        GLES20.glUniform1f(uShowPowerBar, if (showPower) 1f else 0f)
        GLES20.glUniform1f(uPowerFill, powerFill)
        GLES20.glUniform1f(uSpecStrength, spec)
        GLES20.glUniform1f(uAlpha, alpha)

        GLES20.glEnableVertexAttribArray(aPosition)
        mesh.buffer.position(0)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 24, mesh.buffer)

        GLES20.glEnableVertexAttribArray(aNormal)
        mesh.buffer.position(3)
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 24, mesh.buffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aNormal)
    }

    private fun buildProgram(vertSrc: String, fragSrc: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertSrc)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val WORLD_VERT_SHADER = """
            uniform mat4 uMvpMatrix;
            uniform mat4 uModelMatrix;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying vec3 vNormal;
            varying vec3 vLocalPos;
            varying vec3 vWorldPos;
            void main() {
                vLocalPos = aPosition;
                vNormal = normalize(mat3(uModelMatrix) * aNormal);
                vec4 wp = uModelMatrix * vec4(aPosition, 1.0);
                vWorldPos = wp.xyz;
                gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
            }
        """

        private const val WORLD_FRAG_SHADER = """
            precision mediump float;
            uniform vec3 uBaseColor;
            uniform float uShowPowerBar;
            uniform float uPowerFill;
            uniform float uSpecStrength;
            uniform float uAlpha;
            varying vec3 vNormal;
            varying vec3 vLocalPos;
            varying vec3 vWorldPos;

            vec3 powerRamp(float t) {
                vec3 cLow = vec3(0.17, 0.92, 0.72);
                vec3 cMid = vec3(1.00, 0.82, 0.24);
                vec3 cHigh = vec3(1.00, 0.26, 0.44);
                if (t < 0.5) {
                    return mix(cLow, cMid, t * 2.0);
                } else {
                    return mix(cMid, cHigh, (t - 0.5) * 2.0);
                }
            }

            void main() {
                vec3 N = normalize(vNormal);
                vec3 L = normalize(vec3(0.45, 0.85, 0.60));
                vec3 V = normalize(vec3(0.0, 2.5, 14.0) - vWorldPos);
                vec3 H = normalize(L + V);

                float diff = max(dot(N, L), 0.0);
                float hemi = 0.5 + 0.5 * N.y;
                float spec = pow(max(dot(N, H), 0.0), 38.0) * uSpecStrength;
                float rim = pow(1.0 - max(dot(N, V), 0.0), 3.0) * 0.24;

                vec3 albedo = uBaseColor;
                float emissiveBoost = 0.0;

                if (uShowPowerBar > 0.5) {
                    float normY = clamp((vLocalPos.y - 0.02) / 1.66, 0.0, 1.0);
                    float filled = 1.0 - smoothstep(uPowerFill - 0.03, uPowerFill + 0.03, normY);
                    vec3 barCol = mix(uBaseColor, powerRamp(normY), 0.78);
                    vec3 unfillCol = uBaseColor * 0.46 + vec3(0.08, 0.07, 0.14);
                    albedo = mix(unfillCol, barCol, filled);

                    float band = exp(-pow((normY - uPowerFill) * 24.0, 2.0));
                    albedo += vec3(0.95, 0.98, 1.0) * band * 0.55;
                    emissiveBoost = filled * 0.20 + band * 0.30;
                }

                vec3 ambient = albedo * mix(vec3(0.32, 0.30, 0.40), vec3(0.50, 0.49, 0.56), hemi);
                vec3 lit = ambient + albedo * diff * 0.70 + vec3(spec) + vec3(rim) + albedo * emissiveBoost;
                gl_FragColor = vec4(clamp(lit, 0.0, 1.0), uAlpha);
            }
        """

        private const val BG_VERT_SHADER = """
            attribute vec2 aPosition;
            varying float vT;
            void main() {
                vT = aPosition.y * 0.5 + 0.5;
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
        """

        private const val BG_FRAG_SHADER = """
            precision mediump float;
            uniform vec3 uColorTop;
            uniform vec3 uColorBottom;
            varying float vT;
            void main() {
                gl_FragColor = vec4(mix(uColorBottom, uColorTop, vT), 1.0);
            }
        """

        private const val SHADOW_VERT_SHADER = """
            uniform mat4 uMvpMatrix;
            attribute vec3 aPosition;
            varying vec2 vUv;
            void main() {
                vUv = aPosition.xz * 2.0;
                gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
            }
        """

        private const val SHADOW_FRAG_SHADER = """
            precision mediump float;
            uniform float uAlpha;
            varying vec2 vUv;
            void main() {
                float dist = length(vUv);
                float falloff = smoothstep(1.0, 0.1, dist);
                gl_FragColor = vec4(0.04, 0.03, 0.10, falloff * uAlpha);
            }
        """
    }
}
