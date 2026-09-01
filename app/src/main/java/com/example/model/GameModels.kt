package com.example.model

import androidx.compose.ui.graphics.Color

enum class GameState {
    READY,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class BirdSkin(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val bellyColor: Color,
    val beakColor: Color,
    val wingColor: Color,
    val eyeColor: Color,
    val cheekColor: Color,
    val glowColor: Color?
) {
    CLASSIC_YELLOW(
        displayName = "Sunny",
        primaryColor = Color(0xFFF7C820),
        secondaryColor = Color(0xFFD69C00),
        bellyColor = Color(0xFFFFF275),
        beakColor = Color(0xFFFA6800),
        wingColor = Color(0xFFFFFFFF),
        eyeColor = Color(0xFFFFFFFF),
        cheekColor = Color(0xFFFF8DA1),
        glowColor = null
    ),
    RUBY_RED(
        displayName = "Ruby",
        primaryColor = Color(0xFFE53935),
        secondaryColor = Color(0xFFB71C1C),
        bellyColor = Color(0xFFFF8A80),
        beakColor = Color(0xFFFF9800),
        wingColor = Color(0xFFFFEBEE),
        eyeColor = Color(0xFFFFFFFF),
        cheekColor = Color(0xFFFF5252),
        glowColor = null
    ),
    PIP_BLUE(
        displayName = "Pip",
        primaryColor = Color(0xFF039BE5),
        secondaryColor = Color(0xFF01579B),
        bellyColor = Color(0xFF80D8FF),
        beakColor = Color(0xFFFFB300),
        wingColor = Color(0xFFE1F5FE),
        eyeColor = Color(0xFFFFFFFF),
        cheekColor = Color(0xFF4FC3F7),
        glowColor = null
    ),
    GOLDEN_AURA(
        displayName = "Aura",
        primaryColor = Color(0xFFFFD700),
        secondaryColor = Color(0xFFFFA000),
        bellyColor = Color(0xFFFFF9C4),
        beakColor = Color(0xFFFF6F00),
        wingColor = Color(0xFFFFFDE7),
        eyeColor = Color(0xFFFFFFFF),
        cheekColor = Color(0xFFFFE082),
        glowColor = Color(0xFFFFD700)
    )
}

enum class GameTheme(
    val displayName: String,
    val skyTopColor: Color,
    val skyBottomColor: Color,
    val cloudColor: Color,
    val skylineColor: Color,
    val pipeBodyColor: Color,
    val pipeHighlightColor: Color,
    val pipeShadowColor: Color,
    val pipeRimColor: Color,
    val groundTopColor: Color,
    val groundGrassStripeColor: Color,
    val groundDirtColor: Color,
    val isNight: Boolean
) {
    DAY(
        displayName = "Day",
        skyTopColor = Color(0xFF4EC0CA),
        skyBottomColor = Color(0xFF90E8F0),
        cloudColor = Color(0xE6FFFFFF),
        skylineColor = Color(0x7370C5CE),
        pipeBodyColor = Color(0xFF73BF2E),
        pipeHighlightColor = Color(0xFF9DE644),
        pipeShadowColor = Color(0xFF558022),
        pipeRimColor = Color(0xFF2E4E0F),
        groundTopColor = Color(0xFF73BF2E),
        groundGrassStripeColor = Color(0xFF558022),
        groundDirtColor = Color(0xFFDED895),
        isNight = false
    ),
    NIGHT(
        displayName = "Night",
        skyTopColor = Color(0xFF0C1445),
        skyBottomColor = Color(0xFF233268),
        cloudColor = Color(0x40A0B0E0),
        skylineColor = Color(0x60162050),
        pipeBodyColor = Color(0xFF2E8B57),
        pipeHighlightColor = Color(0xFF3CB371),
        pipeShadowColor = Color(0xFF1E5E3A),
        pipeRimColor = Color(0xFF113822),
        groundTopColor = Color(0xFF2E8B57),
        groundGrassStripeColor = Color(0xFF1E5E3A),
        groundDirtColor = Color(0xFF8F886B),
        isNight = true
    ),
    SUNSET(
        displayName = "Sunset",
        skyTopColor = Color(0xFFF07865),
        skyBottomColor = Color(0xFFFDCB6E),
        cloudColor = Color(0x80FFEAA7),
        skylineColor = Color(0x50D63031),
        pipeBodyColor = Color(0xFFE17055),
        pipeHighlightColor = Color(0xFFFAB1A0),
        pipeShadowColor = Color(0xFFB33927),
        pipeRimColor = Color(0xFF631D11),
        groundTopColor = Color(0xFFE17055),
        groundGrassStripeColor = Color(0xFFB33927),
        groundDirtColor = Color(0xFFECCC68),
        isNight = false
    )
}

enum class Medal(val minScore: Int, val displayName: String, val color: Color, val accentColor: Color) {
    NONE(0, "None", Color.Transparent, Color.Transparent),
    BRONZE(10, "Bronze", Color(0xFFCD7F32), Color(0xFFE5A65D)),
    SILVER(20, "Silver", Color(0xFFC0C0C0), Color(0xFFE8E8E8)),
    GOLD(30, "Gold", Color(0xFFFFD700), Color(0xFFFFF07A)),
    PLATINUM(50, "Platinum", Color(0xFF70D6FF), Color(0xFFE0F7FF));

    companion object {
        fun fromScore(score: Int): Medal = when {
            score >= PLATINUM.minScore -> PLATINUM
            score >= GOLD.minScore -> GOLD
            score >= SILVER.minScore -> SILVER
            score >= BRONZE.minScore -> BRONZE
            else -> NONE
        }
    }
}

data class PipeData(
    val id: Long,
    var x: Float,
    val gapY: Float,
    val gapHeight: Float,
    val width: Float = 70f,
    var passed: Boolean = false,
    var hasCoin: Boolean = true,
    var coinCollected: Boolean = false
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float = 1.0f,
    var size: Float,
    val color: Color,
    var life: Float = 1.0f,
    val decay: Float = 0.03f
)

data class WindRipple(
    val x: Float,
    val y: Float,
    var radius: Float = 4f,
    var maxRadius: Float = 36f,
    var alpha: Float = 0.7f,
    val color: Color = Color.White
)

data class ScorePopup(
    val id: Long,
    val text: String,
    val color: Color,
    var yOffset: Float = 0f,
    var alpha: Float = 1f,
    var scale: Float = 0.5f,
    var life: Float = 1f
)

data class AmbientPetal(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var rotation: Float,
    var rotSpeed: Float,
    var alpha: Float,
    val color: Color
)
