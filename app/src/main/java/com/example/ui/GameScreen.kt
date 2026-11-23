package com.example.ui

import android.opengl.GLSurfaceView
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.FlipRun
import com.example.data.UserProfile
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.game.ControlMode
import com.example.game.FlipShape
import com.example.game.FlipSkin
import com.example.game.GamePhase
import com.example.game.GameUiState
import com.example.game.RoomTheme
import com.example.gl.FlipRenderer
import com.example.ui.theme.CoralPop
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MintGlow
import com.example.ui.theme.PowerHigh
import com.example.ui.theme.PowerLow
import com.example.ui.theme.PowerMid
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.WarmCream
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    userDisplayName: String,
    userEmail: String,
    onSignOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showCustomize by viewModel.showCustomizeSheet.collectAsStateWithLifecycle()
    val showHelp by viewModel.showHelpDialog.collectAsStateWithLifecycle()
    val showCloudProfile by viewModel.showCloudProfileSheet.collectAsStateWithLifecycle()
    val cloudProfileState by viewModel.cloudProfileState.collectAsStateWithLifecycle()
    val recentRunsState by viewModel.recentRunsState.collectAsStateWithLifecycle()
    val cloudSyncError by viewModel.cloudSyncError.collectAsStateWithLifecycle()

    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.hapticEvents.collect { event ->
            val constant = when (event) {
                HapticEvent.Launch -> HapticFeedbackConstants.VIRTUAL_KEY
                HapticEvent.LandNormal -> HapticFeedbackConstants.KEYBOARD_TAP
                HapticEvent.LandPerfect -> HapticFeedbackConstants.CONFIRM
                HapticEvent.Crash -> HapticFeedbackConstants.REJECT
            }
            view.performHapticFeedback(constant)
        }
    }

    val renderer = remember {
        FlipRenderer(
            engine = viewModel.engine,
            onFrameTick = { viewModel.refreshUiState() }
        )
    }

    val glSurfaceViewRef = remember { mutableListOf<GLSurfaceView>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepIndigo)
    ) {
        // 3D OpenGL ES Viewport
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(2)
                    setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    glSurfaceViewRef.clear()
                    glSurfaceViewRef.add(this)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_3d_viewport")
                .pointerInput(uiState.phase) {
                    detectTapGestures(
                        onPress = {
                            if (uiState.phase == GamePhase.AIMING || uiState.phase == GamePhase.FLYING) {
                                viewModel.onViewportTap()
                            }
                        }
                    )
                }
        )

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                val glView = glSurfaceViewRef.firstOrNull() ?: return@LifecycleEventObserver
                when (event) {
                    Lifecycle.Event.ON_RESUME -> glView.onResume()
                    Lifecycle.Event.ON_PAUSE -> glView.onPause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        // Safe-area HUD Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            TopHudBar(
                uiState = uiState,
                onToggleSound = viewModel::onToggleSound,
                onOpenCustomize = { viewModel.openCustomizeSheet(true) },
                onOpenCloudProfile = { viewModel.openCloudProfileSheet(true) },
                onOpenHelp = { viewModel.openHelpDialog(true) },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Side Power Meter only appears if user switches to optional Power Bar mode
            if (uiState.controlMode == ControlMode.POWER_BAR &&
                (uiState.phase == GamePhase.AIMING || uiState.phase == GamePhase.FLYING)
            ) {
                PowerMeterSideHud(
                    powerLevel = uiState.powerLevel,
                    sweetZone = uiState.tutorialSweetZone,
                    isAiming = uiState.phase == GamePhase.AIMING,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                )
            }

            // Floating Score & Landing Feedback Popups
            AnimatedVisibility(
                visible = uiState.lastFeedback != null && uiState.phase != GamePhase.TITLE,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = fadeOut() + slideOutVertically { -30 },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp)
            ) {
                uiState.lastFeedback?.let { fb ->
                    val badgeColor = when {
                        fb.isBonus -> ElectricGold
                        fb.isPerfect -> MintGlow
                        else -> WarmCream
                    }
                    Surface(
                        color = MidnightCard.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, badgeColor.copy(alpha = 0.85f)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (fb.isPerfect || fb.isBonus) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = fb.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = badgeColor
                            )
                            Text(
                                text = fb.pointsText,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Gameplay Prompt Pill (subtle hint for first jumps or mid-air flip)
            val showBottomPrompt = when (uiState.phase) {
                GamePhase.AIMING -> uiState.jumpsCompleted < 4 || uiState.controlMode == ControlMode.POWER_BAR
                GamePhase.FLYING -> uiState.airFlipAvailable && (uiState.nextGapNeedsDoubleFlip || uiState.jumpsCompleted < 4)
                else -> false
            }
            AnimatedVisibility(
                visible = showBottomPrompt,
                enter = fadeIn() + slideInVertically { 24 },
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
            ) {
                val promptText = when {
                    uiState.phase == GamePhase.AIMING && uiState.controlMode == ControlMode.POWER_BAR ->
                        "TAP WHEN THE BAR HITS YOUR POWER"
                    uiState.phase == GamePhase.AIMING && uiState.nextGapNeedsDoubleFlip ->
                        "WIDE GAP — TAP THEN TAP AGAIN MID-AIR!"
                    uiState.phase == GamePhase.AIMING ->
                        "TAP ANYWHERE TO FLIP"
                    uiState.airFlipAvailable ->
                        "TAP MID-AIR FOR DOUBLE FLIP!"
                    else -> "STICK THE LANDING!"
                }
                val accent = when {
                    uiState.phase == GamePhase.FLYING && uiState.airFlipAvailable -> MintGlow
                    uiState.nextGapNeedsDoubleFlip -> ElectricGold
                    else -> SoftLavender
                }
                Surface(
                    color = DeepIndigo.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.phase == GamePhase.AIMING) Icons.Filled.TouchApp else Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = promptText,
                            style = MaterialTheme.typography.labelLarge,
                            color = WarmCream
                        )
                    }
                }
            }

            // Title Screen Overlay
            AnimatedVisibility(
                visible = uiState.phase == GamePhase.TITLE,
                enter = fadeIn() + scaleIn(initialScale = 0.94f),
                exit = fadeOut() + scaleOut(targetScale = 0.94f),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                TitleOverlayCard(
                    bestScore = uiState.bestScore,
                    activeSkinName = uiState.selectedSkin.displayName,
                    onPlayClick = viewModel::onStartGame,
                    onCustomizeClick = { viewModel.openCustomizeSheet(true) },
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            // Game Over Overlay
            AnimatedVisibility(
                visible = uiState.phase == GamePhase.GAME_OVER,
                enter = fadeIn() + scaleIn(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                GameOverCard(
                    uiState = uiState,
                    onRetryClick = viewModel::onStartGame,
                    onCustomizeClick = { viewModel.openCustomizeSheet(true) }
                )
            }
        }
    }

    if (showCustomize) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.openCustomizeSheet(false) },
            containerColor = MidnightCard,
            contentColor = WarmCream,
            scrimColor = Color.Black.copy(alpha = 0.18f)
        ) {
            CustomizeSheetContent(
                uiState = uiState,
                onSelectSkin = viewModel::onSelectSkin,
                onSelectTheme = viewModel::onSelectTheme,
                onSelectControlMode = viewModel::onSelectControlMode,
                onToggleHaptics = viewModel::onToggleHaptics,
                onClose = { viewModel.openCustomizeSheet(false) }
            )
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { viewModel.openHelpDialog(false) },
            containerColor = MidnightCard,
            titleContentColor = WarmCream,
            textContentColor = SoftLavender,
            title = {
                Text(
                    text = "How to Play",
                    style = MaterialTheme.typography.headlineMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HelpRuleRow(
                        badge = "1",
                        title = "Tap to Flip",
                        body = "Tap anywhere to jump and perform a clean 360° flip onto the next piece of furniture."
                    )
                    HelpRuleRow(
                        badge = "2",
                        title = "Double-Tap Mid-Air",
                        body = "For wider gaps or higher shelves, tap a second time while airborne for an extra flip boost!"
                    )
                    HelpRuleRow(
                        badge = "3",
                        title = "Endless Score & Streaks",
                        body = "Land upright for Perfect streak multipliers and double points on Gold Pedestals."
                    )
                    HelpRuleRow(
                        badge = "4",
                        title = "Skins & Classic Bottles",
                        body = "Play with the default Penis Skin or switch to Water Bottle, Berry Soda, and Gold Bottle anytime."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.openHelpDialog(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPop)
                ) {
                    Text("Got It!")
                }
            }
        )
    }

    if (showCloudProfile) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.openCloudProfileSheet(false) },
            containerColor = MidnightCard,
            contentColor = WarmCream,
            scrimColor = Color.Black.copy(alpha = 0.28f)
        ) {
            CloudProfileSheetContent(
                userDisplayName = userDisplayName,
                userEmail = userEmail,
                localBestScore = uiState.bestScore,
                localBestStreak = uiState.bestStreak,
                cloudProfileState = cloudProfileState,
                recentRunsState = recentRunsState,
                cloudSyncError = cloudSyncError,
                onDismissError = viewModel::dismissCloudSyncError,
                onDeleteRun = viewModel::onDeleteRun,
                onSignOut = {
                    viewModel.openCloudProfileSheet(false)
                    onSignOut()
                },
                onClose = { viewModel.openCloudProfileSheet(false) }
            )
        }
    }
}

@Composable
private fun TopHudBar(
    uiState: GameUiState,
    onToggleSound: () -> Unit,
    onOpenCustomize: () -> Unit,
    onOpenCloudProfile: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                color = MidnightCard.copy(alpha = 0.80f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGold.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = ElectricGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "BEST ${uiState.bestScore}",
                        style = MaterialTheme.typography.labelLarge,
                        color = WarmCream
                    )
                }
            }

            AnimatedVisibility(visible = uiState.streak >= 2) {
                Surface(
                    color = CoralPop.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = ElectricGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "x${uiState.streak}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }

        if (uiState.phase != GamePhase.TITLE) {
            AnimatedContent(
                targetState = uiState.score,
                transitionSpec = {
                    (scaleIn(initialScale = 0.75f) + fadeIn()) togetherWith fadeOut()
                },
                label = "score_anim"
            ) { currentScore ->
                Surface(
                    color = DeepIndigo.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = currentScore.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = WarmCream,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .testTag("score_text")
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HudCircleButton(
                onClick = onToggleSound,
                contentDescription = stringResource(R.string.cd_sound_toggle),
                testTag = "sound_toggle_button"
            ) {
                Icon(
                    imageVector = if (uiState.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = stringResource(R.string.cd_sound_toggle),
                    tint = if (uiState.soundEnabled) MintGlow else SoftLavender
                )
            }
            HudCircleButton(
                onClick = onOpenCustomize,
                contentDescription = stringResource(R.string.cd_skins_button),
                testTag = "skins_button"
            ) {
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = stringResource(R.string.cd_skins_button),
                    tint = ElectricGold
                )
            }
            HudCircleButton(
                onClick = onOpenCloudProfile,
                contentDescription = stringResource(R.string.cd_cloud_profile_button),
                testTag = "cloud_profile_button"
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = stringResource(R.string.cd_cloud_profile_button),
                    tint = MintGlow
                )
            }
            HudCircleButton(
                onClick = onOpenHelp,
                contentDescription = stringResource(R.string.cd_help_button),
                testTag = "help_button"
            ) {
                Icon(
                    imageVector = Icons.Filled.HelpOutline,
                    contentDescription = stringResource(R.string.cd_help_button),
                    tint = WarmCream
                )
            }
        }
    }
}

@Composable
private fun HudCircleButton(
    onClick: () -> Unit,
    contentDescription: String,
    testTag: String,
    icon: @Composable () -> Unit
) {
    Surface(
        color = MidnightCard.copy(alpha = 0.80f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, SoftLavender.copy(alpha = 0.28f))
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag(testTag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                icon()
            }
        }
    }
}

@Composable
private fun PowerMeterSideHud(
    powerLevel: Float,
    sweetZone: ClosedFloatingPointRange<Float>?,
    isAiming: Boolean,
    modifier: Modifier = Modifier
) {
    val pct = (powerLevel * 100f).roundToInt().coerceIn(0, 100)
    val animatedFill by animateFloatAsState(
        targetValue = powerLevel.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 700f),
        label = "power_fill"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = MidnightCard.copy(alpha = 0.82f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SoftLavender.copy(alpha = 0.30f))
        ) {
            Text(
                text = "$pct%",
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    pct >= 72 -> PowerHigh
                    pct >= 42 -> PowerMid
                    else -> PowerLow
                },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(24.dp)
                .height(190.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MidnightCard.copy(alpha = 0.78f))
                .border(1.5.dp, SoftLavender.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                if (isAiming && sweetZone != null) {
                    val topY = h * (1f - sweetZone.endInclusive)
                    val botY = h * (1f - sweetZone.start)
                    drawRoundRect(
                        color = MintGlow.copy(alpha = 0.28f),
                        topLeft = Offset(0f, topY),
                        size = Size(w, (botY - topY).coerceAtLeast(8f)),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }

                val fillH = h * animatedFill
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(PowerHigh, PowerMid, PowerLow)
                    ),
                    topLeft = Offset(0f, h - fillH),
                    size = Size(w, fillH),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(0f, (h - fillH - 2f).coerceIn(0f, h - 4f)),
                    size = Size(w, 4f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }

        Text(
            text = "POWER",
            style = MaterialTheme.typography.labelSmall,
            color = SoftLavender
        )
    }
}

@Composable
private fun TitleOverlayCard(
    bestScore: Int,
    activeSkinName: String,
    onPlayClick: () -> Unit,
    onCustomizeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 460.dp),
        color = MidnightCard.copy(alpha = 0.90f),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, SoftLavender.copy(alpha = 0.32f)),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "POWER FLIP 3D",
                style = MaterialTheme.typography.displayMedium,
                color = WarmCream,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Tap to flip across endless home furniture, and tap again mid-air for a double flip!",
                style = MaterialTheme.typography.bodyMedium,
                color = SoftLavender,
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = DeepIndigo.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Skin: $activeSkinName",
                        style = MaterialTheme.typography.labelLarge,
                        color = MintGlow,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                if (bestScore > 0) {
                    Surface(
                        color = DeepIndigo.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Best: $bestScore",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricGold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCustomizeClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("title_skins_button"),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Palette,
                        contentDescription = null,
                        tint = ElectricGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Skins", color = WarmCream)
                }

                Button(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .weight(1.6f)
                        .height(54.dp)
                        .testTag("play_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralPop,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "TAP TO PLAY",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun GameOverCard(
    uiState: GameUiState,
    onRetryClick: () -> Unit,
    onCustomizeClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .padding(horizontal = 12.dp),
        color = MidnightCard.copy(alpha = 0.94f),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, CoralPop.copy(alpha = 0.65f)),
        shadowElevation = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiState.isNewBest && uiState.score > 0) {
                Surface(
                    color = ElectricGold,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "NEW HIGH SCORE!",
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepIndigo,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }

            Text(
                text = "FLOPPED!",
                style = MaterialTheme.typography.displayMedium,
                color = CoralPop
            )

            if (uiState.failReason.isNotBlank()) {
                Text(
                    text = uiState.failReason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftLavender
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    label = "SCORE",
                    value = uiState.score.toString(),
                    accent = WarmCream,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "BEST",
                    value = uiState.bestScore.toString(),
                    accent = ElectricGold,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "PERFECTS",
                    value = uiState.perfectCount.toString(),
                    accent = MintGlow,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onRetryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("retry_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralPop,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.cd_retry_button),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "FLIP AGAIN",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            TextButton(
                onClick = onCustomizeClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("gameover_skins_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = null,
                    tint = ElectricGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Skins, Bottles & Rooms", color = SoftLavender)
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = DeepIndigo.copy(alpha = 0.72f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SoftLavender
            )
        }
    }
}

@Composable
private fun CustomizeSheetContent(
    uiState: GameUiState,
    onSelectSkin: (FlipSkin) -> Unit,
    onSelectTheme: (RoomTheme) -> Unit,
    onSelectControlMode: (ControlMode) -> Unit,
    onToggleHaptics: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Skins & Studio",
                style = MaterialTheme.typography.headlineMedium,
                color = WarmCream
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_close_button),
                    tint = SoftLavender
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "CHARACTERS & CLASSIC BOTTLES",
                style = MaterialTheme.typography.labelLarge,
                color = SoftLavender
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(FlipSkin.entries) { skin ->
                    val selected = skin == uiState.selectedSkin
                    val (shapeTag, tagColor) = when (skin.shape) {
                        FlipShape.PENIS_SKIN -> "PENIS" to CoralPop
                        FlipShape.PUSSY_SKIN -> "PUSSY" to Color(0xFFFF7EB6)
                        else -> "BOTTLE" to MintGlow
                    }
                    Surface(
                        modifier = Modifier
                            .width(120.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onSelectSkin(skin) }
                            .testTag("skin_option_${skin.id}"),
                        color = if (selected) DeepIndigo else DeepIndigo.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MintGlow else SoftLavender.copy(alpha = 0.22f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF2F285B),
                                                Color(0xFF191533)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                SkinShapePreview(
                                    skin = skin,
                                    modifier = Modifier
                                        .size(62.dp)
                                        .padding(2.dp)
                                )
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(MintGlow),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = DeepIndigo,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = skin.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = WarmCream,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Text(
                                text = shapeTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = tagColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "ROOM LIGHTING",
                style = MaterialTheme.typography.labelLarge,
                color = SoftLavender
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RoomTheme.entries.forEach { theme ->
                    val selected = theme == uiState.selectedTheme
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectTheme(theme) }
                            .testTag("theme_option_${theme.id}"),
                        color = DeepIndigo.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) ElectricGold else SoftLavender.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(theme.previewTop, theme.previewBottom)
                                        )
                                    )
                            )
                            Text(
                                text = theme.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmCream,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "CONTROL STYLE",
                style = MaterialTheme.typography.labelLarge,
                color = SoftLavender
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ControlMode.entries.forEach { mode ->
                    val selected = mode == uiState.controlMode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectControlMode(mode) }
                            .testTag("control_mode_${mode.id}"),
                        color = if (selected) DeepIndigo else DeepIndigo.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MintGlow else SoftLavender.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MintGlow else WarmCream,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }

        Surface(
            color = DeepIndigo.copy(alpha = 0.55f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Haptic Feedback",
                        style = MaterialTheme.typography.titleMedium,
                        color = WarmCream
                    )
                    Text(
                        text = "Tactile vibration on launch and landings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftLavender,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = uiState.hapticsEnabled,
                    onCheckedChange = { onToggleHaptics() },
                    modifier = Modifier.testTag("haptics_switch")
                )
            }
        }
    }
}

@Composable
private fun HelpRuleRow(badge: String, title: String, body: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(CoralPop),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = WarmCream
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = SoftLavender
            )
        }
    }
}

private fun FloatArray.toComposeColor(alpha: Float = 1f): Color =
    Color(this[0].coerceIn(0f, 1f), this[1].coerceIn(0f, 1f), this[2].coerceIn(0f, 1f), alpha)

@Composable
private fun SkinShapePreview(skin: FlipSkin, modifier: Modifier = Modifier) {
    val bodyColor = skin.bodyRgb.toComposeColor()
    val capColor = skin.capRgb.toComposeColor()
    val accentColor = skin.accentRgb.toComposeColor()
    val shadowColor = Color.Black.copy(alpha = 0.30f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft platform shadow underneath each preview
        drawOval(
            color = shadowColor,
            topLeft = Offset(w * 0.22f, h * 0.87f),
            size = Size(w * 0.56f, h * 0.09f)
        )

        when (skin.shape) {
            FlipShape.PENIS_SKIN -> {
                // Twin rounded base spheres
                val lobeR = w * 0.165f
                drawCircle(
                    color = bodyColor,
                    radius = lobeR,
                    center = Offset(w * 0.35f, h * 0.76f)
                )
                drawCircle(
                    color = bodyColor,
                    radius = lobeR,
                    center = Offset(w * 0.65f, h * 0.76f)
                )
                // Main shaft
                drawRoundRect(
                    color = bodyColor,
                    topLeft = Offset(w * 0.365f, h * 0.29f),
                    size = Size(w * 0.27f, h * 0.51f),
                    cornerRadius = CornerRadius(w * 0.13f, w * 0.13f)
                )
                // Rounded cap dome
                drawOval(
                    color = capColor,
                    topLeft = Offset(w * 0.34f, h * 0.10f),
                    size = Size(w * 0.32f, h * 0.26f)
                )
                // Collar ring
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(w * 0.335f, h * 0.275f),
                    size = Size(w * 0.33f, h * 0.065f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                )
                // Cute cartoon eyes
                drawCircle(Color.White, radius = w * 0.046f, center = Offset(w * 0.44f, h * 0.46f))
                drawCircle(Color.White, radius = w * 0.046f, center = Offset(w * 0.56f, h * 0.46f))
                drawCircle(Color(0xFF1A1628), radius = w * 0.024f, center = Offset(w * 0.445f, h * 0.46f))
                drawCircle(Color(0xFF1A1628), radius = w * 0.024f, center = Offset(w * 0.565f, h * 0.46f))
                // Glossy highlight
                drawLine(
                    color = Color.White.copy(alpha = 0.45f),
                    start = Offset(w * 0.41f, h * 0.18f),
                    end = Offset(w * 0.41f, h * 0.64f),
                    strokeWidth = w * 0.032f,
                    cap = StrokeCap.Round
                )
            }

            FlipShape.PUSSY_SKIN -> {
                // Sculpted twin peach/heart cushion silhouette
                drawOval(
                    color = bodyColor,
                    topLeft = Offset(w * 0.18f, h * 0.16f),
                    size = Size(w * 0.41f, h * 0.72f)
                )
                drawOval(
                    color = bodyColor,
                    topLeft = Offset(w * 0.41f, h * 0.16f),
                    size = Size(w * 0.41f, h * 0.72f)
                )
                // Inner cleft petals
                drawOval(
                    color = accentColor,
                    topLeft = Offset(w * 0.37f, h * 0.34f),
                    size = Size(w * 0.26f, h * 0.44f)
                )
                // Central vertical groove
                drawOval(
                    color = capColor,
                    topLeft = Offset(w * 0.465f, h * 0.39f),
                    size = Size(w * 0.07f, h * 0.33f)
                )
                // Top pearl crest
                drawCircle(
                    color = capColor,
                    radius = w * 0.068f,
                    center = Offset(w * 0.50f, h * 0.345f)
                )
                // Cute eyes
                drawCircle(Color.White, radius = w * 0.044f, center = Offset(w * 0.37f, h * 0.28f))
                drawCircle(Color.White, radius = w * 0.044f, center = Offset(w * 0.63f, h * 0.28f))
                drawCircle(Color(0xFF1A1628), radius = w * 0.023f, center = Offset(w * 0.37f, h * 0.28f))
                drawCircle(Color(0xFF1A1628), radius = w * 0.023f, center = Offset(w * 0.63f, h * 0.28f))
                // Glossy highlight
                drawLine(
                    color = Color.White.copy(alpha = 0.40f),
                    start = Offset(w * 0.27f, h * 0.34f),
                    end = Offset(w * 0.25f, h * 0.65f),
                    strokeWidth = w * 0.032f,
                    cap = StrokeCap.Round
                )
            }

            FlipShape.WATER_BOTTLE -> {
                // Bottle shoulder & body
                val bodyPath = Path().apply {
                    moveTo(w * 0.42f, h * 0.21f)
                    lineTo(w * 0.58f, h * 0.21f)
                    lineTo(w * 0.59f, h * 0.29f)
                    lineTo(w * 0.69f, h * 0.39f)
                    lineTo(w * 0.69f, h * 0.85f)
                    quadraticTo(w * 0.69f, h * 0.89f, w * 0.63f, h * 0.89f)
                    lineTo(w * 0.37f, h * 0.89f)
                    quadraticTo(w * 0.31f, h * 0.89f, w * 0.31f, h * 0.85f)
                    lineTo(w * 0.31f, h * 0.39f)
                    lineTo(w * 0.41f, h * 0.29f)
                    close()
                }
                drawPath(bodyPath, color = bodyColor)
                // White label band
                drawRect(
                    color = accentColor,
                    topLeft = Offset(w * 0.31f, h * 0.49f),
                    size = Size(w * 0.38f, h * 0.17f)
                )
                // Ribbed grip lines
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(w * 0.33f, h * 0.74f),
                    end = Offset(w * 0.67f, h * 0.74f),
                    strokeWidth = w * 0.022f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(w * 0.33f, h * 0.80f),
                    end = Offset(w * 0.67f, h * 0.80f),
                    strokeWidth = w * 0.022f
                )
                // Blue screw cap
                drawRoundRect(
                    color = capColor,
                    topLeft = Offset(w * 0.40f, h * 0.11f),
                    size = Size(w * 0.20f, h * 0.11f),
                    cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
                )
                // Plastic highlight
                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = Offset(w * 0.37f, h * 0.41f),
                    end = Offset(w * 0.37f, h * 0.83f),
                    strokeWidth = w * 0.032f,
                    cap = StrokeCap.Round
                )
            }

            FlipShape.SODA_BOTTLE -> {
                // Contoured soda bottle silhouette with pinched waist
                val sodaPath = Path().apply {
                    moveTo(w * 0.43f, h * 0.20f)
                    lineTo(w * 0.57f, h * 0.20f)
                    lineTo(w * 0.58f, h * 0.30f)
                    cubicTo(w * 0.67f, h * 0.38f, w * 0.70f, h * 0.48f, w * 0.66f, h * 0.62f)
                    cubicTo(w * 0.64f, h * 0.69f, w * 0.69f, h * 0.77f, w * 0.68f, h * 0.86f)
                    quadraticTo(w * 0.68f, h * 0.89f, w * 0.62f, h * 0.89f)
                    lineTo(w * 0.38f, h * 0.89f)
                    quadraticTo(w * 0.32f, h * 0.89f, w * 0.32f, h * 0.86f)
                    cubicTo(w * 0.31f, h * 0.77f, w * 0.36f, h * 0.69f, w * 0.34f, h * 0.62f)
                    cubicTo(w * 0.30f, h * 0.48f, w * 0.33f, h * 0.38f, w * 0.42f, h * 0.30f)
                    close()
                }
                drawPath(sodaPath, color = bodyColor)
                // Label band
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(w * 0.34f, h * 0.47f),
                    size = Size(w * 0.32f, h * 0.14f),
                    cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
                )
                // Crown cap
                drawRoundRect(
                    color = capColor,
                    topLeft = Offset(w * 0.405f, h * 0.12f),
                    size = Size(w * 0.19f, h * 0.09f),
                    cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.45f),
                    start = Offset(w * 0.38f, h * 0.40f),
                    end = Offset(w * 0.38f, h * 0.82f),
                    strokeWidth = w * 0.028f,
                    cap = StrokeCap.Round
                )
            }

            FlipShape.TROPHY_BOTTLE -> {
                // Champagne / trophy bottle silhouette
                val trophyPath = Path().apply {
                    moveTo(w * 0.43f, h * 0.17f)
                    lineTo(w * 0.57f, h * 0.17f)
                    lineTo(w * 0.60f, h * 0.36f)
                    cubicTo(w * 0.70f, h * 0.44f, w * 0.71f, h * 0.54f, w * 0.70f, h * 0.86f)
                    quadraticTo(w * 0.70f, h * 0.89f, w * 0.64f, h * 0.89f)
                    lineTo(w * 0.36f, h * 0.89f)
                    quadraticTo(w * 0.30f, h * 0.89f, w * 0.30f, h * 0.86f)
                    cubicTo(w * 0.29f, h * 0.54f, w * 0.30f, h * 0.44f, w * 0.40f, h * 0.36f)
                    close()
                }
                drawPath(trophyPath, color = bodyColor)
                // Gold foil neck & cork
                val foilPath = Path().apply {
                    moveTo(w * 0.42f, h * 0.11f)
                    lineTo(w * 0.58f, h * 0.11f)
                    lineTo(w * 0.60f, h * 0.37f)
                    lineTo(w * 0.40f, h * 0.37f)
                    close()
                }
                drawPath(foilPath, color = capColor)
                // Gold crest badge
                drawOval(
                    color = accentColor,
                    topLeft = Offset(w * 0.40f, h * 0.52f),
                    size = Size(w * 0.20f, h * 0.18f),
                    style = Stroke(width = w * 0.03f)
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.50f),
                    start = Offset(w * 0.36f, h * 0.46f),
                    end = Offset(w * 0.36f, h * 0.83f),
                    strokeWidth = w * 0.030f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun CloudProfileSheetContent(
    userDisplayName: String,
    userEmail: String,
    localBestScore: Int,
    localBestStreak: Int,
    cloudProfileState: CloudUiState<UserProfile?>,
    recentRunsState: CloudUiState<List<FlipRun>>,
    cloudSyncError: String?,
    onDismissError: () -> Unit,
    onDeleteRun: (String) -> Unit,
    onSignOut: () -> Unit,
    onClose: () -> Unit
) {
    val profile = (cloudProfileState as? CloudUiState.Success)?.data
    val bestScore = maxOf(localBestScore, profile?.bestScore?.toInt() ?: 0)
    val bestStreak = maxOf(localBestStreak, profile?.bestStreak?.toInt() ?: 0)
    val totalRuns = profile?.totalRuns?.toInt() ?: 0
    val totalPerfects = profile?.totalPerfects?.toInt() ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CoralPop.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userDisplayName.firstOrNull()?.uppercase() ?: "F",
                        style = MaterialTheme.typography.titleLarge,
                        color = CoralPop,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = profile?.displayName?.ifBlank { userDisplayName } ?: userDisplayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = WarmCream,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (userEmail.isNotBlank()) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SoftLavender,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_close_button),
                    tint = SoftLavender
                )
            }
        }

        Surface(
            color = DeepIndigo.copy(alpha = 0.65f),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MintGlow.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudDone,
                    contentDescription = null,
                    tint = MintGlow,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = when (cloudProfileState) {
                        is CloudUiState.Loading -> "Syncing with Cloud Firestore…"
                        is CloudUiState.Error -> "Cloud sync issue — local stats kept safe"
                        is CloudUiState.Success -> "Connected to Cloud Firestore"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (cloudProfileState is CloudUiState.Error) ElectricGold else MintGlow
                )
            }
        }

        if (cloudSyncError != null) {
            Surface(
                color = CoralPop.copy(alpha = 0.16f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CoralPop.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = CoralPop,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = cloudSyncError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmCream,
                            fontSize = 12.sp
                        )
                    }
                    TextButton(onClick = onDismissError) {
                        Text("Dismiss", color = ElectricGold)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile(
                label = "BEST",
                value = bestScore.toString(),
                accent = ElectricGold,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "STREAK",
                value = bestStreak.toString(),
                accent = CoralPop,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "RUNS",
                value = totalRuns.toString(),
                accent = MintGlow,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "PERFECTS",
                value = totalPerfects.toString(),
                accent = WarmCream,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = stringResource(R.string.cloud_recent_runs),
            style = MaterialTheme.typography.labelLarge,
            color = SoftLavender
        )

        when (recentRunsState) {
            is CloudUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MintGlow,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            is CloudUiState.Error -> {
                Surface(
                    color = DeepIndigo.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = recentRunsState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoralPop,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            is CloudUiState.Success -> {
                val runs = recentRunsState.data
                if (runs.isEmpty()) {
                    Surface(
                        color = DeepIndigo.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = SoftLavender
                            )
                            Text(
                                text = stringResource(R.string.cloud_no_runs),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SoftLavender
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        runs.take(8).forEach { run ->
                            CloudRunRow(
                                run = run,
                                onDelete = { onDeleteRun(run.id) }
                            )
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("sign_out_button"),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CoralPop.copy(alpha = 0.65f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = CoralPop,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.cloud_sign_out),
                style = MaterialTheme.typography.titleMedium,
                color = CoralPop
            )
        }
    }
}

@Composable
private fun CloudRunRow(
    run: FlipRun,
    onDelete: () -> Unit
) {
    val skin = run.resolvedSkin()
    val dateLabel = remember(run.createdAt) {
        val date = run.createdAt?.toDate()
        if (date != null) {
            SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(date)
        } else {
            "Just now"
        }
    }

    Surface(
        color = DeepIndigo.copy(alpha = 0.65f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SoftLavender.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = ElectricGold.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${run.score}",
                        style = MaterialTheme.typography.titleLarge,
                        color = ElectricGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Column {
                    Text(
                        text = "${skin.displayName} • ${run.jumpsCompleted} jumps",
                        style = MaterialTheme.typography.labelLarge,
                        color = WarmCream,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Streak x${run.maxStreak} • ${run.perfectCount} perfects • $dateLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftLavender,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_run_${run.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_run),
                    tint = SoftLavender
                )
            }
        }
    }
}
