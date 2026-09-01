package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.ScoreEntity
import com.example.model.BirdSkin
import com.example.model.GameState
import com.example.model.GameTheme
import com.example.model.Medal
import com.example.viewmodel.GameViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlappyBirdGameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val topScores by viewModel.topScores.collectAsState()
    val totalGames by viewModel.totalGamesPlayed.collectAsState()
    val totalCoins by viewModel.totalCoinsCollected.collectAsState()

    val density = LocalDensity.current.density
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()

    // 60-120 FPS high performance game loop
    var lastFrameTimeNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos > 0L) {
                    val deltaSeconds = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
                    viewModel.update(deltaSeconds)
                }
                lastFrameTimeNanos = frameTimeNanos
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(uiState.gameState) {
                detectTapGestures(
                    onPress = {
                        viewModel.handleTap()
                    }
                )
            }
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        LaunchedEffect(widthPx, heightPx, density) {
            viewModel.onScreenSizeChanged(widthPx, heightPx, density)
        }

        // 1. GAME RENDERING CANVAS (Draw-phase isolated for true 120 FPS buttery smooth motion)
        GameCanvas(
            viewModel = viewModel,
            theme = uiState.selectedTheme,
            skin = uiState.selectedSkin,
            density = density,
            modifier = Modifier.fillMaxSize()
        )

        // 2. TOP HUD (Score, Coins, Pause, Sound)
        TopGameHud(
            uiState = uiState,
            onPause = { viewModel.pauseGame() },
            onToggleMute = { viewModel.toggleMute() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(safeInsets)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 3. READY STATE OVERLAY
        if (uiState.gameState == GameState.READY) {
            ReadyScreenOverlay(
                highScore = uiState.highScore,
                onOpenStats = { viewModel.setShowStats(true) },
                onOpenSkins = { viewModel.setShowSkin(true) },
                onOpenThemes = { viewModel.setShowTheme(true) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(safeInsets)
            )
        }

        // 4. PAUSE OVERLAY
        if (uiState.gameState == GameState.PAUSED) {
            PauseOverlay(
                onResume = { viewModel.resumeGame() },
                onRestart = { viewModel.restartGame() }
            )
        }

        // 5. GAME OVER OVERLAY
        if (uiState.gameState == GameState.GAME_OVER) {
            GameOverOverlay(
                score = uiState.score,
                highScore = uiState.highScore,
                coins = uiState.coins,
                isNewBest = uiState.isNewHighScore,
                medal = uiState.earnedMedal,
                onPlayAgain = { viewModel.startGame() },
                onOpenStats = { viewModel.setShowStats(true) },
                onOpenSkins = { viewModel.setShowSkin(true) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(safeInsets)
            )
        }

        // 6. STATS & LEADERBOARD SHEET
        if (uiState.showStatsDialog) {
            StatsBottomSheet(
                highScore = uiState.highScore,
                totalGames = totalGames,
                totalCoins = totalCoins ?: 0,
                topScores = topScores,
                onDismiss = { viewModel.setShowStats(false) },
                onResetScores = { viewModel.resetAllStats() }
            )
        }

        // 7. SKIN SELECTOR DIALOG
        if (uiState.showSkinDialog) {
            SkinSelectorDialog(
                currentSkin = uiState.selectedSkin,
                onSelectSkin = { skin ->
                    viewModel.selectSkin(skin)
                    viewModel.setShowSkin(false)
                },
                onDismiss = { viewModel.setShowSkin(false) }
            )
        }

        // 8. THEME SELECTOR DIALOG
        if (uiState.showThemeDialog) {
            ThemeSelectorDialog(
                currentTheme = uiState.selectedTheme,
                onSelectTheme = { theme ->
                    viewModel.selectTheme(theme)
                    viewModel.setShowTheme(false)
                },
                onDismiss = { viewModel.setShowTheme(false) }
            )
        }

        // 9. 3-SECOND WELCOME & LOADING SCREEN (Created by CHAOS)
        AnimatedVisibility(
            visible = uiState.isWelcomeLoading,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(400))
        ) {
            WelcomeLoadingOverlay(
                progress = uiState.welcomeProgress,
                skin = uiState.selectedSkin,
                onSkip = { viewModel.onWelcomeFinished() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun WelcomeLoadingOverlay(
    progress: Float,
    skin: BirdSkin,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_anim")
    val birdBob by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "birdBob"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    val percent = (progress * 100f).toInt().coerceIn(0, 100)

    Box(
        modifier = modifier
            .testTag("welcome_loading_screen")
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1424),
                        Color(0xFF1A1C38),
                        Color(0xFF0B0D18)
                    )
                )
            )
            .clickable(onClick = onSkip),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        ) {
            // Floating Bird in Glowing Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(130.dp)
                    .offset(y = birdBob.dp)
            ) {
                // Outer Pulsing Halo
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = glowPulse * 0.45f),
                                    Color(0xFF4EC0CA).copy(alpha = glowPulse * 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Inner Glow Sphere
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF2C3E50), Color(0xFF1A252F))
                            )
                        )
                        .border(
                            3.dp,
                            Brush.sweepGradient(
                                listOf(Color(0xFFFFD700), Color(0xFF4EC0CA), Color(0xFFFFD700))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    BirdPreviewIcon(skin = skin, sizeDp = 48.dp)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Main Game Title
            Text(
                text = "FLAPPY BIRD",
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFFFF275),
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // "CREATED BY CHAOS" Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFF8F00).copy(alpha = 0.9f),
                                Color(0xFFFFD54F).copy(alpha = shimmerAlpha),
                                Color(0xFFFF8F00).copy(alpha = 0.9f)
                            )
                        )
                    )
                    .border(2.dp, Color(0xFF543847), RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "CREATED BY CHAOS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF2D1B00),
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tagline: "Just Enjoy"
            Text(
                text = "✨ Just Enjoy ✨",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF90E8F0).copy(alpha = shimmerAlpha),
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(42.dp))

            // 3-Second Loading Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFF141829))
                        .border(2.dp, Color(0xFF543847), RoundedCornerShape(9.dp))
                        .padding(2.5.dp)
                ) {
                    // Filled Progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(13.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF00E676),
                                        Color(0xFF00E5FF),
                                        Color(0xFFFFD700)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "READY TO FLY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xCC90E8F0)
                    )
                    Text(
                        text = "$percent%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFD700)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "TAP SCREEN TO START",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = Color(0x66FFFFFF),
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun GameCanvas(
    viewModel: GameViewModel,
    theme: GameTheme,
    skin: BirdSkin,
    density: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.testTag("flappy_game_canvas")
    ) {
        // Read drawTick in the DrawScope only: skips Compose tree recompositions and directly executes 120 FPS draw passes
        val tick = viewModel.drawTick.longValue
        if (tick < 0) return@Canvas
        FlappyRenderer.drawGame(
            drawScope = this,
            width = size.width,
            height = size.height,
            density = density,
            theme = theme,
            skin = skin,
            birdX = viewModel.birdX,
            birdY = viewModel.birdY,
            birdRotation = viewModel.birdRotation,
            birdWingFrame = viewModel.birdWingFrame,
            birdRadiusDp = viewModel.birdRadiusDp,
            birdScaleX = viewModel.birdScaleX,
            birdScaleY = viewModel.birdScaleY,
            groundY = viewModel.groundY,
            groundScrollX = viewModel.groundScrollX,
            cloudScrollX = viewModel.cloudScrollX,
            cityScrollX = viewModel.cityScrollX,
            pipes = viewModel.pipes,
            particles = viewModel.particles,
            windRipples = viewModel.windRipples,
            scorePopups = viewModel.scorePopups,
            ambientPetals = viewModel.ambientPetals,
            flashEffect = viewModel.flashEffect,
            screenShake = viewModel.screenShake
        )
    }
}

@Composable
private fun TopGameHud(
    uiState: com.example.viewmodel.GameUiState,
    onPause: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left controls: Sound toggle
        RetroIconButton(
            icon = if (uiState.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
            onClick = onToggleMute,
            contentDescription = "Toggle Mute",
            size = 42.dp,
            testTag = "sound_toggle_button"
        )

        // Center: Live Score in playing/paused state
        if (uiState.gameState == GameState.PLAYING || uiState.gameState == GameState.PAUSED) {
            ArcadeScoreText(
                score = uiState.score,
                fontSize = 44,
                modifier = Modifier.testTag("live_score_display")
            )
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        // Right controls: Coins badge + Pause button
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinBadge(coins = uiState.coins)
            if (uiState.gameState == GameState.PLAYING) {
                Spacer(modifier = Modifier.width(8.dp))
                RetroIconButton(
                    icon = Icons.Default.Pause,
                    onClick = onPause,
                    contentDescription = "Pause Game",
                    size = 42.dp,
                    testTag = "pause_game_button"
                )
            }
        }
    }
}

@Composable
private fun ReadyScreenOverlay(
    highScore: Int,
    onOpenStats: () -> Unit,
    onOpenSkins: () -> Unit,
    onOpenThemes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Space for HUD
        Spacer(modifier = Modifier.height(60.dp))

        // Center Brand Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-40).dp)
        ) {
            // Retro Title Card
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xDD543847))
                    .border(3.dp, Color(0xFFF7C820), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "FLAPPY BIRD",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFFFF275),
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Best Score Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xCC000000),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF9DE644))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BEST: $highScore",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Animated Tap to Flap Prompt
            TapToFlapPrompt()
        }

        // Bottom Action Bar: Skins, Themes, Stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 120.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RetroArcadeButton(
                text = "SKINS",
                icon = Icons.Default.Face,
                onClick = onOpenSkins,
                baseColor = Color(0xFF2E7D32),
                topColor = Color(0xFF66BB6A),
                modifier = Modifier.width(100.dp),
                height = 46.dp,
                testTag = "open_skins_button"
            )
            RetroArcadeButton(
                text = "THEME",
                icon = Icons.Default.WbSunny,
                onClick = onOpenThemes,
                baseColor = Color(0xFF1565C0),
                topColor = Color(0xFF42A5F5),
                modifier = Modifier.width(100.dp),
                height = 46.dp,
                testTag = "open_theme_button"
            )
            RetroArcadeButton(
                text = "STATS",
                icon = Icons.Default.BarChart,
                onClick = onOpenStats,
                baseColor = Color(0xFF6A1B9A),
                topColor = Color(0xFFAB47BC),
                modifier = Modifier.width(100.dp),
                height = 46.dp,
                testTag = "open_stats_button"
            )
        }
    }
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFDED895)),
            border = androidx.compose.foundation.BorderStroke(4.dp, Color(0xFF543847)),
            modifier = Modifier
                .widthIn(max = 320.dp)
                .padding(24.dp)
                .shadow(12.dp, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PAUSED",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF543847),
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                RetroArcadeButton(
                    text = "RESUME",
                    icon = Icons.Default.PlayArrow,
                    onClick = onResume,
                    baseColor = Color(0xFF73BF2E),
                    topColor = Color(0xFF9DE644),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "resume_button"
                )

                Spacer(modifier = Modifier.height(14.dp))

                RetroArcadeButton(
                    text = "RESTART",
                    icon = Icons.Default.Refresh,
                    onClick = onRestart,
                    baseColor = Color(0xFFE86100),
                    topColor = Color(0xFFFFA53C),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "restart_button"
                )
            }
        }
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    highScore: Int,
    coins: Int,
    isNewBest: Boolean,
    medal: Medal,
    onPlayAgain: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSkins: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 350.dp)
                .padding(horizontal = 20.dp)
        ) {
            // GAME OVER Header
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF543847))
                    .border(3.dp, Color(0xFFE86100), RoundedCornerShape(14.dp))
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "GAME OVER",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFFFA53C),
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scoreboard Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDED895)),
                border = androidx.compose.foundation.BorderStroke(3.5.dp, Color(0xFF543847)),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Medal Box
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "MEDAL",
                                color = Color(0xFF543847),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            MedalBadge(medal = medal)
                            if (medal != Medal.NONE) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = medal.displayName.uppercase(),
                                    color = Color(0xFF543847),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Right: Score & Best
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "SCORE",
                                color = Color(0xFF543847),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = score.toString(),
                                color = Color(0xFF543847),
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isNewBest) {
                                    Surface(
                                        color = Color(0xFFE53935),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        Text(
                                            text = "NEW",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "BEST",
                                    color = Color(0xFF543847),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = highScore.toString(),
                                color = Color(0xFF543847),
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    HorizontalDivider(
                        color = Color(0xFF543847).copy(alpha = 0.3f),
                        thickness = 2.dp,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )

                    // Round Coins summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COINS COLLECTED",
                            color = Color(0xFF543847),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "+$coins",
                                color = Color(0xFFB8860B),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "★", color = Color(0xFFFFD700), fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            RetroArcadeButton(
                text = "PLAY AGAIN",
                icon = Icons.Default.PlayArrow,
                onClick = onPlayAgain,
                baseColor = Color(0xFF73BF2E),
                topColor = Color(0xFF9DE644),
                modifier = Modifier.fillMaxWidth(),
                height = 54.dp,
                testTag = "play_again_button"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RetroArcadeButton(
                    text = "STATS",
                    icon = Icons.Default.BarChart,
                    onClick = onOpenStats,
                    baseColor = Color(0xFF6A1B9A),
                    topColor = Color(0xFFAB47BC),
                    modifier = Modifier.weight(1f),
                    height = 48.dp,
                    testTag = "game_over_stats_button"
                )
                RetroArcadeButton(
                    text = "SKINS",
                    icon = Icons.Default.Face,
                    onClick = onOpenSkins,
                    baseColor = Color(0xFF1565C0),
                    topColor = Color(0xFF42A5F5),
                    modifier = Modifier.weight(1f),
                    height = 48.dp,
                    testTag = "game_over_skins_button"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsBottomSheet(
    highScore: Int,
    totalGames: Int,
    totalCoins: Int,
    topScores: List<ScoreEntity>,
    onDismiss: () -> Unit,
    onResetScores: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF2C2C2C),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF888888))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color(0xFFFFF275),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LEADERBOARD & STATS",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "BEST SCORE",
                    value = highScore.toString(),
                    color = Color(0xFFFFD700),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "GAMES",
                    value = totalGames.toString(),
                    color = Color(0xFF64B5F6),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "COINS",
                    value = totalCoins.toString(),
                    color = Color(0xFFFFB74D),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "TOP 10 FLIGHTS",
                color = Color(0xFFFFF275),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (topScores.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No games played yet!\nTap to start flapping.",
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(topScores) { index, record ->
                        val medal = Medal.fromScore(record.score)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF383838),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (index == 0) Color(0xFFFFD700) else Color(0x33FFFFFF)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${index + 1}",
                                        color = if (index == 0) Color(0xFFFFD700) else Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(32.dp)
                                    )
                                    if (medal != Medal.NONE) {
                                        Text(
                                            text = "🏅",
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "${record.score} PTS",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = dateFormat.format(Date(record.timestamp)),
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "+${record.coins} ★",
                                        color = Color(0xFFFFD700),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            RetroArcadeButton(
                text = "RESET ALL DATA",
                onClick = onResetScores,
                baseColor = Color(0xFFC62828),
                topColor = Color(0xFFEF5350),
                modifier = Modifier.fillMaxWidth(),
                height = 46.dp,
                testTag = "reset_stats_button"
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF383838),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun SkinSelectorDialog(
    currentSkin: BirdSkin,
    onSelectSkin: (BirdSkin) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
            border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF543847)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SELECT BIRD SKIN",
                    color = Color(0xFFFFF275),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(18.dp))

                BirdSkin.entries.forEach { skin ->
                    val isSelected = skin == currentSkin
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFF484848) else Color(0xFF363636),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            if (isSelected) Color(0xFF9DE644) else Color(0x33FFFFFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable { onSelectSkin(skin) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Mini Bird Preview
                                BirdPreviewIcon(skin = skin, sizeDp = 38.dp)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = skin.displayName,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (skin == BirdSkin.GOLDEN_AURA) {
                                        Text(
                                            text = "★ Special Sparkle Trail",
                                            color = Color(0xFFFFD700),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            if (isSelected) {
                                Text(
                                    text = "EQUIPPED",
                                    color = Color(0xFF9DE644),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                RetroArcadeButton(
                    text = "DONE",
                    onClick = onDismiss,
                    baseColor = Color(0xFF1565C0),
                    topColor = Color(0xFF42A5F5),
                    modifier = Modifier.fillMaxWidth(),
                    height = 46.dp
                )
            }
        }
    }
}

@Composable
fun ThemeSelectorDialog(
    currentTheme: GameTheme,
    onSelectTheme: (GameTheme) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
            border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF543847)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SELECT THEME",
                    color = Color(0xFFFFF275),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(18.dp))

                GameTheme.entries.forEach { theme ->
                    val isSelected = theme == currentTheme
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFF484848) else Color(0xFF363636),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            if (isSelected) Color(0xFF9DE644) else Color(0x33FFFFFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable { onSelectTheme(theme) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Theme gradient preview
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(theme.skyTopColor, theme.skyBottomColor)
                                            )
                                        )
                                        .border(2.dp, Color(0xFF543847), RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = theme.displayName,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (isSelected) {
                                Text(
                                    text = "SELECTED",
                                    color = Color(0xFF9DE644),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                RetroArcadeButton(
                    text = "DONE",
                    onClick = onDismiss,
                    baseColor = Color(0xFF1565C0),
                    topColor = Color(0xFF42A5F5),
                    modifier = Modifier.fillMaxWidth(),
                    height = 46.dp
                )
            }
        }
    }
}
