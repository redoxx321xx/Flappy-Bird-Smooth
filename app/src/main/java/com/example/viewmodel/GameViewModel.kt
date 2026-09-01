package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.db.AppDatabase
import com.example.data.db.ScoreEntity
import com.example.data.db.ScoreRepository
import com.example.model.BirdSkin
import com.example.model.GameState
import com.example.model.GameTheme
import com.example.model.Medal
import com.example.model.Particle
import com.example.model.PipeData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class GameUiState(
    val gameState: GameState = GameState.READY,
    val score: Int = 0,
    val coins: Int = 0,
    val highScore: Int = 0,
    val isNewHighScore: Boolean = false,
    val selectedSkin: BirdSkin = BirdSkin.CLASSIC_YELLOW,
    val selectedTheme: GameTheme = GameTheme.DAY,
    val isMuted: Boolean = false,
    val earnedMedal: Medal = Medal.NONE,
    val showStatsDialog: Boolean = false,
    val showSkinDialog: Boolean = false,
    val showThemeDialog: Boolean = false
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ScoreRepository(database.scoreDao())
    val soundManager = SoundManager()

    val topScores: StateFlow<List<ScoreEntity>> = repository.topScores.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentScores: StateFlow<List<ScoreEntity>> = repository.recentScores.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val dbHighScore: StateFlow<Int?> = repository.highScore.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val totalGamesPlayed: StateFlow<Int> = repository.totalGames.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val totalCoinsCollected: StateFlow<Int?> = repository.totalCoins.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // 120 FPS high-rate render trigger (read solely in Canvas DrawScope to avoid UI recomposition)
    val drawTick = mutableLongStateOf(0L)

    // Visual impact effects (updated directly per frame without recomposing UI hierarchy)
    var flashEffect: Float = 0f
    var screenShake: Float = 0f

    // Game Physics State (in DP / scale independent units)
    var screenWidthPx: Float = 1080f
    var screenHeightPx: Float = 2400f
    var density: Float = 2.75f

    // Bird state
    var birdX: Float = 0f
    var birdY: Float = 0f
    var birdVelocityY: Float = 0f
    var birdRotation: Float = 0f
    var birdWingFrame: Float = 0f // 0f..3f
    val birdRadiusDp: Float = 18f

    // Ground and Parallax
    var groundY: Float = 0f
    var groundScrollX: Float = 0f
    var cloudScrollX: Float = 0f
    var cityScrollX: Float = 0f

    // Pipes and Particles
    val pipes = mutableListOf<PipeData>()
    val particles = mutableListOf<Particle>()

    private var nextPipeId: Long = 0
    private var idleBobTime: Float = 0f

    init {
        viewModelScope.launch {
            repository.highScore.collect { hs ->
                val current = hs ?: 0
                _uiState.value = _uiState.value.copy(highScore = current)
            }
        }
    }

    fun onScreenSizeChanged(width: Float, height: Float, displayDensity: Float) {
        screenWidthPx = width
        screenHeightPx = height
        density = displayDensity

        groundY = screenHeightPx - (112f * density)
        birdX = screenWidthPx * 0.28f

        if (_uiState.value.gameState == GameState.READY) {
            resetBirdPosition()
        }
    }

    private fun resetBirdPosition() {
        birdY = groundY * 0.48f
        birdVelocityY = 0f
        birdRotation = 0f
        birdWingFrame = 0f
    }

    fun handleTap() {
        when (_uiState.value.gameState) {
            GameState.READY -> {
                startGame()
            }
            GameState.PLAYING -> {
                flap()
            }
            GameState.PAUSED -> {
                resumeGame()
            }
            GameState.GAME_OVER -> {
                // Tapping on game over card is handled by buttons
            }
        }
    }

    fun startGame() {
        pipes.clear()
        particles.clear()
        resetBirdPosition()
        flashEffect = 0f
        screenShake = 0f
        _uiState.value = _uiState.value.copy(
            gameState = GameState.PLAYING,
            score = 0,
            coins = 0,
            isNewHighScore = false,
            earnedMedal = Medal.NONE
        )
        soundManager.playSwoosh()
        flap()
    }

    fun flap() {
        if (_uiState.value.gameState != GameState.PLAYING) return
        val jumpImpulse = -430f * density
        birdVelocityY = jumpImpulse
        birdRotation = -24f
        soundManager.playFlap()

        // Generate feather/sparkle particles on flap if Aura skin
        if (_uiState.value.selectedSkin == BirdSkin.GOLDEN_AURA) {
            createFlapParticles()
        }
    }

    fun pauseGame() {
        if (_uiState.value.gameState == GameState.PLAYING) {
            _uiState.value = _uiState.value.copy(gameState = GameState.PAUSED)
            soundManager.playClick()
        }
    }

    fun resumeGame() {
        if (_uiState.value.gameState == GameState.PAUSED) {
            _uiState.value = _uiState.value.copy(gameState = GameState.PLAYING)
            soundManager.playClick()
        }
    }

    fun restartGame() {
        soundManager.playClick()
        resetToReady()
    }

    fun resetToReady() {
        pipes.clear()
        particles.clear()
        resetBirdPosition()
        flashEffect = 0f
        screenShake = 0f
        _uiState.value = _uiState.value.copy(
            gameState = GameState.READY,
            score = 0,
            coins = 0,
            isNewHighScore = false,
            earnedMedal = Medal.NONE
        )
    }

    fun toggleMute() {
        val newMute = !_uiState.value.isMuted
        soundManager.isMuted = newMute
        _uiState.value = _uiState.value.copy(isMuted = newMute)
        if (!newMute) soundManager.playClick()
    }

    fun selectSkin(skin: BirdSkin) {
        _uiState.value = _uiState.value.copy(selectedSkin = skin)
        soundManager.playClick()
    }

    fun selectTheme(theme: GameTheme) {
        _uiState.value = _uiState.value.copy(selectedTheme = theme)
        soundManager.playClick()
    }

    fun setShowStats(show: Boolean) {
        _uiState.value = _uiState.value.copy(showStatsDialog = show)
        soundManager.playClick()
    }

    fun setShowSkin(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSkinDialog = show)
        soundManager.playClick()
    }

    fun setShowTheme(show: Boolean) {
        _uiState.value = _uiState.value.copy(showThemeDialog = show)
        soundManager.playClick()
    }

    fun resetAllStats() {
        viewModelScope.launch {
            repository.resetScores()
            _uiState.value = _uiState.value.copy(highScore = 0)
            soundManager.playClick()
        }
    }

    // High performance 120 FPS game update loop
    fun update(deltaSeconds: Float) {
        val dt = deltaSeconds.coerceIn(0.001f, 0.033f)

        // Decrease flash and shake effects without recomposing UI state
        if (flashEffect > 0f) {
            flashEffect = (flashEffect - dt * 3.5f).coerceAtLeast(0f)
        }
        if (screenShake > 0f) {
            screenShake = (screenShake - dt * 4.0f).coerceAtLeast(0f)
        }

        // Parallax environment update
        val groundSpeed = 160f * density
        val cloudSpeed = 25f * density
        val citySpeed = 45f * density

        cloudScrollX = (cloudScrollX + cloudSpeed * dt) % (screenWidthPx * 2)
        cityScrollX = (cityScrollX + citySpeed * dt) % (screenWidthPx * 2)

        val currentState = _uiState.value.gameState

        if (currentState == GameState.READY) {
            // Idle bobbing
            idleBobTime += dt * 6f
            birdY = (groundY * 0.48f) + sin(idleBobTime.toDouble()).toFloat() * (10f * density)
            birdWingFrame = (birdWingFrame + dt * 10f) % 3f
            groundScrollX = (groundScrollX + groundSpeed * dt) % (24f * density)
            drawTick.longValue++
            return
        }

        if (currentState != GameState.PLAYING) {
            // If Game Over / Paused, update particles only
            updateParticles(dt)
            drawTick.longValue++
            return
        }

        groundScrollX = (groundScrollX + groundSpeed * dt) % (24f * density)
        birdWingFrame = (birdWingFrame + dt * 12f) % 3f

        // Physics: Bird Gravity & Velocity
        val gravity = 1350f * density
        birdVelocityY += gravity * dt
        birdY += birdVelocityY * dt

        // Bird Rotation: tilt upwards on jump, dive smoothly when falling
        if (birdVelocityY < 0) {
            birdRotation = (-25f).coerceAtLeast(birdRotation - dt * 180f)
        } else {
            birdRotation = (birdRotation + dt * 380f).coerceAtMost(80f)
        }

        // Pipes Logic: Spawning & Movement
        val pipeSpeed = 165f * density
        val pipeWidth = 66f * density
        val pipeGapHeight = 160f * density
        val pipeSpacing = 220f * density

        // Spawn pipes
        val rightMostPipeX = pipes.maxOfOrNull { it.x } ?: 0f
        if (pipes.isEmpty() || screenWidthPx - rightMostPipeX >= pipeSpacing) {
            spawnPipe(pipeWidth, pipeGapHeight)
        }

        // Move pipes & check score / coin collision
        val birdRadiusPx = birdRadiusDp * density
        val birdCenterY = birdY
        val birdCenterX = birdX

        val iterator = pipes.iterator()
        while (iterator.hasNext()) {
            val pipe = iterator.next()
            pipe.x -= pipeSpeed * dt

            // Scoring check (when pipe crosses bird)
            if (!pipe.passed && pipe.x + pipe.width < birdCenterX) {
                pipe.passed = true
                val newScore = _uiState.value.score + 1
                val isNewBest = newScore > _uiState.value.highScore
                _uiState.value = _uiState.value.copy(
                    score = newScore,
                    isNewHighScore = isNewBest
                )
                soundManager.playPoint()
                createScoreParticles(birdCenterX, birdCenterY)
            }

            // Coin collection check
            if (pipe.hasCoin && !pipe.coinCollected) {
                val coinX = pipe.x + pipe.width / 2f
                val coinY = pipe.gapY + pipe.gapHeight / 2f
                val coinRadius = 14f * density

                val dx = birdCenterX - coinX
                val dy = birdCenterY - coinY
                val distSq = dx * dx + dy * dy
                val combinedRadius = birdRadiusPx + coinRadius

                if (distSq <= combinedRadius * combinedRadius) {
                    pipe.coinCollected = true
                    _uiState.value = _uiState.value.copy(coins = _uiState.value.coins + 1)
                    soundManager.playCoin()
                    createCoinParticles(coinX, coinY)
                }
            }

            // Remove off-screen pipes
            if (pipe.x + pipe.width < -50f) {
                iterator.remove()
            }
        }

        // Collision Check: Bird with Top/Bottom boundaries and Pipes
        if (checkCollisions(birdCenterX, birdCenterY, birdRadiusPx)) {
            triggerGameOver()
            drawTick.longValue++
            return
        }

        // Update active particles
        updateParticles(dt)

        // Trigger draw phase update
        drawTick.longValue++
    }

    private fun spawnPipe(width: Float, gapHeight: Float) {
        val minGapY = 80f * density
        val maxGapY = groundY - gapHeight - (60f * density)
        val gapY = Random.nextFloat() * (maxGapY - minGapY) + minGapY
        val hasCoin = Random.nextFloat() > 0.35f // 65% chance of bonus coin

        pipes.add(
            PipeData(
                id = nextPipeId++,
                x = screenWidthPx + 20f,
                gapY = gapY,
                gapHeight = gapHeight,
                width = width,
                hasCoin = hasCoin
            )
        )
    }

    private fun checkCollisions(birdCenterX: Float, birdCenterY: Float, birdRadiusPx: Float): Boolean {
        // Ground collision
        if (birdCenterY + birdRadiusPx >= groundY) {
            birdY = groundY - birdRadiusPx
            return true
        }

        // Ceiling collision
        if (birdCenterY - birdRadiusPx <= 0f) {
            birdY = birdRadiusPx
            return true
        }

        // Pipe collision (Circle vs AABB with hit-box inset for fair gameplay)
        val hitPadding = 4f * density
        for (pipe in pipes) {
            val pipeLeft = pipe.x + hitPadding
            val pipeRight = pipe.x + pipe.width - hitPadding

            // Top pipe rect: [pipeLeft, 0, pipeRight, pipe.gapY]
            if (circleIntersectsRect(
                    birdCenterX, birdCenterY, birdRadiusPx * 0.88f,
                    pipeLeft, 0f, pipeRight, pipe.gapY
                )
            ) {
                return true
            }

            // Bottom pipe rect: [pipeLeft, pipe.gapY + pipe.gapHeight, pipeRight, groundY]
            val bottomPipeTop = pipe.gapY + pipe.gapHeight
            if (circleIntersectsRect(
                    birdCenterX, birdCenterY, birdRadiusPx * 0.88f,
                    pipeLeft, bottomPipeTop, pipeRight, groundY
                )
            ) {
                return true
            }
        }
        return false
    }

    private fun circleIntersectsRect(
        cx: Float, cy: Float, radius: Float,
        rx1: Float, ry1: Float, rx2: Float, ry2: Float
    ): Boolean {
        val nearestX = cx.coerceIn(rx1, rx2)
        val nearestY = cy.coerceIn(ry1, ry2)
        val dx = cx - nearestX
        val dy = cy - nearestY
        return (dx * dx + dy * dy) < (radius * radius)
    }

    private fun triggerGameOver() {
        soundManager.playHit()
        val score = _uiState.value.score
        val coins = _uiState.value.coins
        val skin = _uiState.value.selectedSkin.name
        val theme = _uiState.value.selectedTheme.name
        val earnedMedal = Medal.fromScore(score)

        val newHigh = score > _uiState.value.highScore
        val finalHighScore = if (newHigh) score else _uiState.value.highScore

        flashEffect = 1.0f
        screenShake = 1.0f

        _uiState.value = _uiState.value.copy(
            gameState = GameState.GAME_OVER,
            highScore = finalHighScore,
            isNewHighScore = newHigh,
            earnedMedal = earnedMedal
        )

        // Save score to Room database
        viewModelScope.launch {
            repository.saveScore(score, coins, skin, theme)
        }

        // Create crash particle burst
        createCrashParticles(birdX, birdY)
    }

    private fun updateParticles(dt: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += 300f * density * dt
            p.life -= p.decay * (dt * 60f)
            p.alpha = p.life.coerceIn(0f, 1f)
            if (p.life <= 0f) {
                iterator.remove()
            }
        }
    }

    private fun createCoinParticles(x: Float, y: Float) {
        if (particles.size > 40) return
        val colors = listOf(Color(0xFFFFD700), Color(0xFFFFF9C4), Color(0xFFFFA000), Color.White)
        for (i in 0 until 12) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = (Random.nextFloat() * 140f + 60f) * density
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    size = (Random.nextFloat() * 4f + 3f) * density,
                    color = colors.random(),
                    decay = 0.04f
                )
            )
        }
    }

    private fun createScoreParticles(x: Float, y: Float) {
        if (particles.size > 40) return
        for (i in 0 until 6) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = (Random.nextFloat() * 80f + 30f) * density
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    size = (Random.nextFloat() * 4f + 2.5f) * density,
                    color = Color.White,
                    decay = 0.045f
                )
            )
        }
    }

    private fun createCrashParticles(x: Float, y: Float) {
        val skin = _uiState.value.selectedSkin
        val colors = listOf(skin.primaryColor, skin.beakColor, skin.wingColor, Color.White)
        for (i in 0 until 18) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = (Random.nextFloat() * 200f + 50f) * density
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    size = (Random.nextFloat() * 5f + 3f) * density,
                    color = colors.random(),
                    decay = 0.03f
                )
            )
        }
    }

    private fun createFlapParticles() {
        if (particles.size > 30) return
        for (i in 0 until 2) {
            particles.add(
                Particle(
                    x = birdX - 10f * density,
                    y = birdY + (Random.nextFloat() * 10f - 5f) * density,
                    vx = -(Random.nextFloat() * 70f + 30f) * density,
                    vy = (Random.nextFloat() * 30f - 15f) * density,
                    size = (Random.nextFloat() * 3f + 2f) * density,
                    color = Color(0xFFFFE082),
                    decay = 0.06f
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
