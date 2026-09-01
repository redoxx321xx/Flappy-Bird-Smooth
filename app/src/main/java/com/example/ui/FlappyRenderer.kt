package com.example.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.example.model.BirdSkin
import com.example.model.GameTheme
import com.example.model.Particle
import com.example.model.PipeData
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object FlappyRenderer {

    // Reusable Path instances to avoid GC allocations during high-framerate rendering
    private val beakPath = Path()
    private val lowerBeakPath = Path()
    private val stripePath = Path()

    // Pre-allocated static layout tables
    // Building tuples: (bx_dp, bh_dp, bw_dp)
    private val BUILDING_DEFS = floatArrayOf(
        0f, 90f, 34f,
        34f, 130f, 40f,
        74f, 70f, 30f,
        104f, 110f, 46f,
        150f, 60f, 28f,
        178f, 140f, 44f,
        222f, 85f, 36f,
        258f, 120f, 38f,
        296f, 75f, 24f
    )

    // Cloud tuples: (cx_dp, cy_fraction, cw_dp)
    private val CLOUD_DEFS = floatArrayOf(
        40f, 0.28f, 75f,
        170f, 0.18f, 95f,
        310f, 0.35f, 65f
    )

    // Star normalized coordinates: (x_fraction, y_dp)
    private val STAR_DEFS = floatArrayOf(
        0.12f, 80f,
        0.28f, 130f,
        0.45f, 60f,
        0.62f, 110f,
        0.70f, 40f,
        0.90f, 190f,
        0.18f, 220f,
        0.52f, 240f
    )

    // Cached gradient brushes per theme
    private var cachedTheme: GameTheme? = null
    private var cachedGroundY: Float = -1f
    private var cachedSkyBrush: Brush? = null

    private fun getSkyBrush(theme: GameTheme, groundY: Float): Brush {
        if (theme == cachedTheme && cachedGroundY == groundY && cachedSkyBrush != null) {
            return cachedSkyBrush!!
        }
        cachedTheme = theme
        cachedGroundY = groundY
        val brush = Brush.verticalGradient(
            colors = listOf(theme.skyTopColor, theme.skyBottomColor),
            startY = 0f,
            endY = groundY
        )
        cachedSkyBrush = brush
        return brush
    }

    fun drawGame(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        density: Float,
        theme: GameTheme,
        skin: BirdSkin,
        birdX: Float,
        birdY: Float,
        birdRotation: Float,
        birdWingFrame: Float,
        birdRadiusDp: Float,
        groundY: Float,
        groundScrollX: Float,
        cloudScrollX: Float,
        cityScrollX: Float,
        pipes: List<PipeData>,
        particles: List<Particle>,
        flashEffect: Float,
        screenShake: Float
    ) {
        val shakeX = if (screenShake > 0f) (sin(System.currentTimeMillis() * 0.08) * screenShake * 16f * density).toFloat() else 0f
        val shakeY = if (screenShake > 0f) (cos(System.currentTimeMillis() * 0.08) * screenShake * 16f * density).toFloat() else 0f

        drawScope.translate(left = shakeX, top = shakeY) {
            // 1. Sky Gradient Background
            drawRect(
                brush = getSkyBrush(theme, groundY),
                size = Size(width, height)
            )

            // If Night, draw glowing moon and twinkling stars
            if (theme.isNight) {
                drawMoonAndStars(width, density)
            }

            // 2. Parallax City Skyline / Hills
            drawCitySkyline(width, groundY, cityScrollX, theme.skylineColor, density)

            // 3. Parallax Clouds
            drawClouds(width, groundY, cloudScrollX, theme.cloudColor, density)

            // 4. Pipes
            drawPipes(pipes, groundY, theme, density)

            // 5. Coins
            drawCoins(pipes, density)

            // 6. Particles
            drawParticles(particles)

            // 7. Ground
            drawGround(width, height, groundY, groundScrollX, theme, density)

            // 8. Bird
            drawBird(birdX, birdY, birdRotation, birdWingFrame, birdRadiusDp * density, skin, density)

            // 9. Screen Flash
            if (flashEffect > 0.01f) {
                drawRect(
                    color = Color.White.copy(alpha = (flashEffect * 0.85f).coerceIn(0f, 1f)),
                    size = Size(width, height)
                )
            }
        }
    }

    private fun DrawScope.drawMoonAndStars(width: Float, density: Float) {
        // Moon
        val moonX = width * 0.82f
        val moonY = 110f * density
        val moonRadius = 26f * density

        // Moon glow
        drawCircle(
            color = Color(0x33FFFDE7),
            radius = moonRadius * 1.5f,
            center = Offset(moonX, moonY)
        )
        // Moon body
        drawCircle(
            color = Color(0xFFFFFDE7),
            radius = moonRadius,
            center = Offset(moonX, moonY)
        )
        // Moon crater / crescent shading
        drawCircle(
            color = Color(0xFFF0ECB8),
            radius = moonRadius * 0.35f,
            center = Offset(moonX - 6f * density, moonY + 4f * density)
        )
        drawCircle(
            color = Color(0xFFF0ECB8),
            radius = moonRadius * 0.22f,
            center = Offset(moonX + 8f * density, moonY - 6f * density)
        )

        // Stars
        var i = 0
        while (i < STAR_DEFS.size) {
            val starX = width * STAR_DEFS[i]
            val starY = STAR_DEFS[i + 1] * density
            drawCircle(
                color = Color(0xD0FFFFFF),
                radius = 2.5f * density,
                center = Offset(starX, starY)
            )
            i += 2
        }
    }

    private fun DrawScope.drawCitySkyline(
        width: Float,
        groundY: Float,
        scrollX: Float,
        color: Color,
        density: Float
    ) {
        val segmentWidth = 320f * density
        val totalSegments = (width / segmentWidth).toInt() + 3
        val baseOffset = -(scrollX % segmentWidth)

        for (s in -1 until totalSegments) {
            val offsetX = baseOffset + s * segmentWidth
            var i = 0
            while (i < BUILDING_DEFS.size) {
                val bx = BUILDING_DEFS[i] * density
                val bh = BUILDING_DEFS[i + 1] * density
                val bw = BUILDING_DEFS[i + 2] * density
                i += 3

                drawRect(
                    color = color,
                    topLeft = Offset(offsetX + bx, groundY - bh),
                    size = Size(bw, bh)
                )
                // Roof antenna or peak
                if (bh > 100f * density) {
                    drawRect(
                        color = color,
                        topLeft = Offset(offsetX + bx + bw / 2 - 2f * density, groundY - bh - 14f * density),
                        size = Size(4f * density, 14f * density)
                    )
                }
            }
        }
    }

    private fun DrawScope.drawClouds(
        width: Float,
        groundY: Float,
        scrollX: Float,
        color: Color,
        density: Float
    ) {
        val cloudPatternWidth = 400f * density
        val numPatterns = (width / cloudPatternWidth).toInt() + 3
        val baseOffset = -(scrollX % cloudPatternWidth)

        for (p in -1 until numPatterns) {
            val offset = baseOffset + p * cloudPatternWidth
            var i = 0
            while (i < CLOUD_DEFS.size) {
                val cx = CLOUD_DEFS[i] * density
                val cy = groundY * CLOUD_DEFS[i + 1]
                val cw = CLOUD_DEFS[i + 2] * density
                i += 3

                val cloudX = offset + cx
                val cloudY = cy
                val r = cw * 0.35f

                // Fluffy cloud circles
                drawCircle(color = color, radius = r * 0.75f, center = Offset(cloudX, cloudY))
                drawCircle(color = color, radius = r, center = Offset(cloudX + r * 0.9f, cloudY - r * 0.25f))
                drawCircle(color = color, radius = r * 0.85f, center = Offset(cloudX + r * 1.8f, cloudY))
                drawRoundRect(
                    color = color,
                    topLeft = Offset(cloudX - r * 0.4f, cloudY - r * 0.1f),
                    size = Size(r * 2.6f, r * 1.1f),
                    cornerRadius = CornerRadius(r * 0.5f, r * 0.5f)
                )
            }
        }
    }

    private fun DrawScope.drawPipes(
        pipes: List<PipeData>,
        groundY: Float,
        theme: GameTheme,
        density: Float
    ) {
        val rimHeight = 24f * density
        val rimExtension = 4f * density
        val outlineColor = theme.pipeRimColor
        val outlineWidth = 2.5f * density

        for (pipe in pipes) {
            val pipeX = pipe.x
            val pipeW = pipe.width
            val gapTop = pipe.gapY
            val gapBottom = pipe.gapY + pipe.gapHeight

            // ==================== TOP PIPE ====================
            val topBodyBottom = gapTop - rimHeight
            if (topBodyBottom > 0) {
                drawPipeSection(
                    x = pipeX,
                    y = -10f,
                    width = pipeW,
                    height = topBodyBottom + 10f,
                    theme = theme,
                    density = density,
                    outlineColor = outlineColor,
                    outlineWidth = outlineWidth
                )
            }
            // Top Pipe Collar / Rim
            drawPipeRim(
                x = pipeX - rimExtension,
                y = gapTop - rimHeight,
                width = pipeW + (rimExtension * 2f),
                height = rimHeight,
                theme = theme,
                density = density,
                outlineColor = outlineColor,
                outlineWidth = outlineWidth,
                isTopPipe = true
            )

            // ==================== BOTTOM PIPE ====================
            // Bottom Pipe Collar / Rim
            drawPipeRim(
                x = pipeX - rimExtension,
                y = gapBottom,
                width = pipeW + (rimExtension * 2f),
                height = rimHeight,
                theme = theme,
                density = density,
                outlineColor = outlineColor,
                outlineWidth = outlineWidth,
                isTopPipe = false
            )
            // Bottom Pipe Body
            val bottomBodyTop = gapBottom + rimHeight
            if (groundY > bottomBodyTop) {
                drawPipeSection(
                    x = pipeX,
                    y = bottomBodyTop,
                    width = pipeW,
                    height = groundY - bottomBodyTop + 10f,
                    theme = theme,
                    density = density,
                    outlineColor = outlineColor,
                    outlineWidth = outlineWidth
                )
            }
        }
    }

    private fun DrawScope.drawPipeSection(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        theme: GameTheme,
        density: Float,
        outlineColor: Color,
        outlineWidth: Float
    ) {
        // Outline border
        drawRect(
            color = outlineColor,
            topLeft = Offset(x, y),
            size = Size(width, height)
        )

        val innerX = x + outlineWidth
        val innerW = width - (outlineWidth * 2f)

        // Main Body Fill
        drawRect(
            color = theme.pipeBodyColor,
            topLeft = Offset(innerX, y),
            size = Size(innerW, height)
        )

        // Left Highlight Strip
        val hlW = innerW * 0.16f
        val hlX = innerX + innerW * 0.10f
        drawRect(
            color = theme.pipeHighlightColor,
            topLeft = Offset(hlX, y),
            size = Size(hlW, height)
        )

        // Left Fine Specular Line
        drawRect(
            color = Color.White.copy(alpha = 0.55f),
            topLeft = Offset(hlX + 2f * density, y),
            size = Size(2f * density, height)
        )

        // Right Shadow Strip
        val shadowW = innerW * 0.26f
        val shadowX = innerX + innerW - shadowW
        drawRect(
            color = theme.pipeShadowColor,
            topLeft = Offset(shadowX, y),
            size = Size(shadowW, height)
        )
    }

    private fun DrawScope.drawPipeRim(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        theme: GameTheme,
        density: Float,
        outlineColor: Color,
        outlineWidth: Float,
        isTopPipe: Boolean
    ) {
        val corner = CornerRadius(3f * density, 3f * density)

        // Outer Dark Outline
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(x, y),
            size = Size(width, height),
            cornerRadius = corner
        )

        val innerX = x + outlineWidth
        val innerY = y + outlineWidth
        val innerW = width - (outlineWidth * 2f)
        val innerH = height - (outlineWidth * 2f)

        // Inner Main Rim Body
        drawRoundRect(
            color = theme.pipeBodyColor,
            topLeft = Offset(innerX, innerY),
            size = Size(innerW, innerH),
            cornerRadius = CornerRadius(2f * density, 2f * density)
        )

        // Highlight Strip
        val hlW = innerW * 0.16f
        val hlX = innerX + innerW * 0.10f
        drawRect(
            color = theme.pipeHighlightColor,
            topLeft = Offset(hlX, innerY),
            size = Size(hlW, innerH)
        )
        drawRect(
            color = Color.White.copy(alpha = 0.6f),
            topLeft = Offset(hlX + 2f * density, innerY),
            size = Size(2f * density, innerH)
        )

        // Shadow Strip
        val shadowW = innerW * 0.26f
        val shadowX = innerX + innerW - shadowW
        drawRect(
            color = theme.pipeShadowColor,
            topLeft = Offset(shadowX, innerY),
            size = Size(shadowW, innerH)
        )

        // Edge Lip bevel
        val lipY = if (isTopPipe) innerY + innerH - (3f * density) else innerY
        drawRect(
            color = if (isTopPipe) theme.pipeShadowColor else theme.pipeHighlightColor.copy(alpha = 0.8f),
            topLeft = Offset(innerX, lipY),
            size = Size(innerW, 3f * density)
        )
    }

    private fun DrawScope.drawCoins(pipes: List<PipeData>, density: Float) {
        val time = System.currentTimeMillis()
        val spin = (time % 1200) / 1200f // 0 to 1
        val scaleX = abs(cos(spin * 2 * PI)).toFloat().coerceAtLeast(0.15f)

        for (pipe in pipes) {
            if (pipe.hasCoin && !pipe.coinCollected) {
                val coinX = pipe.x + pipe.width / 2f
                val coinY = pipe.gapY + pipe.gapHeight / 2f
                val radius = 13f * density

                // Gold coin outer border
                drawOval(
                    color = Color(0xFF7A5200),
                    topLeft = Offset(coinX - radius * scaleX, coinY - radius),
                    size = Size(radius * 2f * scaleX, radius * 2f)
                )

                // Gold coin inner fill
                drawOval(
                    color = Color(0xFFFFD700),
                    topLeft = Offset(coinX - (radius - 2f * density) * scaleX, coinY - (radius - 2f * density)),
                    size = Size((radius - 2f * density) * 2f * scaleX, (radius - 2f * density) * 2f)
                )

                // Coin star / core embossing
                if (scaleX > 0.45f) {
                    drawOval(
                        color = Color(0xFFFFA000),
                        topLeft = Offset(coinX - (radius * 0.55f) * scaleX, coinY - (radius * 0.55f)),
                        size = Size((radius * 0.55f) * 2f * scaleX, radius * 1.1f)
                    )
                    // Specular sparkle
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = 2.5f * density * scaleX,
                        center = Offset(coinX - radius * 0.35f * scaleX, coinY - radius * 0.35f)
                    )
                }
            }
        }
    }

    private fun DrawScope.drawGround(
        width: Float,
        height: Float,
        groundY: Float,
        groundScrollX: Float,
        theme: GameTheme,
        density: Float
    ) {
        val darkBorder = Color(0xFF543847)

        // 1. Dark separator line
        drawRect(
            color = darkBorder,
            topLeft = Offset(0f, groundY),
            size = Size(width, 3.5f * density)
        )

        // 2. Green Grass Top Band
        val grassH = 14f * density
        drawRect(
            color = theme.groundTopColor,
            topLeft = Offset(0f, groundY + 3.5f * density),
            size = Size(width, grassH)
        )

        // 3. Diagonal Grass Stripes (classic animated retro pattern)
        val stripeUnit = 24f * density
        val stripeOffset = -(groundScrollX % stripeUnit)
        val totalStripes = (width / stripeUnit).toInt() + 3

        for (i in -1 until totalStripes) {
            val sx = stripeOffset + i * stripeUnit
            stripePath.reset()
            stripePath.moveTo(sx, groundY + 3.5f * density)
            stripePath.lineTo(sx + 10f * density, groundY + 3.5f * density)
            stripePath.lineTo(sx + 4f * density, groundY + 3.5f * density + grassH)
            stripePath.lineTo(sx - 6f * density, groundY + 3.5f * density + grassH)
            stripePath.close()
            drawPath(stripePath, color = theme.groundGrassStripeColor)
        }

        // 4. Grass bottom dark line
        drawRect(
            color = darkBorder,
            topLeft = Offset(0f, groundY + 3.5f * density + grassH),
            size = Size(width, 2.5f * density)
        )

        // 5. Dirt Sand Base
        val dirtTop = groundY + 3.5f * density + grassH + 2.5f * density
        drawRect(
            color = theme.groundDirtColor,
            topLeft = Offset(0f, dirtTop),
            size = Size(width, height - dirtTop)
        )

        // Dirt Texture specks
        val speckColor = darkBorder.copy(alpha = 0.25f)
        val speckStep = 40f * density
        val totalSpecks = (width / speckStep).toInt() + 2
        for (i in 0 until totalSpecks) {
            val sx = (i * speckStep - groundScrollX * 0.8f) % (width + speckStep)
            drawRect(
                color = speckColor,
                topLeft = Offset(sx, dirtTop + 14f * density),
                size = Size(6f * density, 3f * density)
            )
            drawRect(
                color = speckColor,
                topLeft = Offset(sx + 20f * density, dirtTop + 32f * density),
                size = Size(8f * density, 3.5f * density)
            )
        }
    }

    private fun DrawScope.drawBird(
        bx: Float,
        by: Float,
        rotationDeg: Float,
        wingFrame: Float,
        radius: Float,
        skin: BirdSkin,
        density: Float
    ) {
        val darkOutline = Color(0xFF543847)
        val outlineW = 2.2f * density

        rotate(degrees = rotationDeg, pivot = Offset(bx, by)) {
            // Optional Aura Glow
            if (skin.glowColor != null) {
                drawCircle(
                    color = skin.glowColor.copy(alpha = 0.35f),
                    radius = radius * 1.55f,
                    center = Offset(bx, by)
                )
            }

            // 1. Bird Main Body Outline
            drawCircle(
                color = darkOutline,
                radius = radius + outlineW,
                center = Offset(bx, by)
            )

            // 2. Main Bird Body Fill
            drawCircle(
                color = skin.primaryColor,
                radius = radius,
                center = Offset(bx, by)
            )

            // 3. Belly highlight
            drawCircle(
                color = skin.bellyColor,
                radius = radius * 0.75f,
                center = Offset(bx + radius * 0.15f, by + radius * 0.25f)
            )

            // 4. Rosy Cheek
            drawCircle(
                color = skin.cheekColor,
                radius = radius * 0.22f,
                center = Offset(bx + radius * 0.15f, by + radius * 0.35f)
            )

            // 5. Eye
            val eyeX = bx + radius * 0.45f
            val eyeY = by - radius * 0.30f
            val eyeRadius = radius * 0.42f

            // Eye outline
            drawCircle(
                color = darkOutline,
                radius = eyeRadius + outlineW,
                center = Offset(eyeX, eyeY)
            )
            // Eye White
            drawCircle(
                color = skin.eyeColor,
                radius = eyeRadius,
                center = Offset(eyeX, eyeY)
            )
            // Eye Pupil
            val pupilX = eyeX + eyeRadius * 0.28f
            val pupilY = eyeY - eyeRadius * 0.05f
            drawCircle(
                color = Color.Black,
                radius = eyeRadius * 0.42f,
                center = Offset(pupilX, pupilY)
            )
            // Pupil Highlight
            drawCircle(
                color = Color.White,
                radius = eyeRadius * 0.16f,
                center = Offset(pupilX - 1.5f * density, pupilY - 1.5f * density)
            )

            // 6. Beak
            val beakStartX = bx + radius * 0.7f
            val beakTopY = by - radius * 0.05f

            // Top Beak
            beakPath.reset()
            beakPath.moveTo(beakStartX, beakTopY)
            beakPath.lineTo(beakStartX + radius * 0.65f, beakTopY + radius * 0.2f)
            beakPath.lineTo(beakStartX, beakTopY + radius * 0.35f)
            beakPath.close()

            drawPath(
                path = beakPath,
                color = darkOutline,
                style = Stroke(width = outlineW * 1.5f)
            )
            drawPath(
                path = beakPath,
                color = skin.beakColor,
                style = Fill
            )

            // Lower Beak
            lowerBeakPath.reset()
            lowerBeakPath.moveTo(beakStartX, beakTopY + radius * 0.30f)
            lowerBeakPath.lineTo(beakStartX + radius * 0.5f, beakTopY + radius * 0.38f)
            lowerBeakPath.lineTo(beakStartX, beakTopY + radius * 0.55f)
            lowerBeakPath.close()

            drawPath(
                path = lowerBeakPath,
                color = darkOutline,
                style = Stroke(width = outlineW * 1.5f)
            )
            drawPath(
                path = lowerBeakPath,
                color = skin.beakColor,
                style = Fill
            )

            // 7. Flapping Wing
            val wingAngle = when (wingFrame.toInt() % 3) {
                0 -> -35f
                1 -> 0f
                else -> 35f
            }

            val wingCenterX = bx - radius * 0.35f
            val wingCenterY = by + radius * 0.1f

            rotate(degrees = wingAngle, pivot = Offset(wingCenterX, wingCenterY)) {
                val wingW = radius * 0.85f
                val wingH = radius * 0.55f

                // Wing outline
                drawRoundRect(
                    color = darkOutline,
                    topLeft = Offset(wingCenterX - wingW * 0.5f - outlineW, wingCenterY - wingH * 0.5f - outlineW),
                    size = Size(wingW + outlineW * 2f, wingH + outlineW * 2f),
                    cornerRadius = CornerRadius(wingH * 0.5f, wingH * 0.5f)
                )

                // Wing base
                drawRoundRect(
                    color = skin.wingColor,
                    topLeft = Offset(wingCenterX - wingW * 0.5f, wingCenterY - wingH * 0.5f),
                    size = Size(wingW, wingH),
                    cornerRadius = CornerRadius(wingH * 0.5f, wingH * 0.5f)
                )

                // Wing bottom shadow
                drawRoundRect(
                    color = skin.secondaryColor.copy(alpha = 0.4f),
                    topLeft = Offset(wingCenterX - wingW * 0.5f, wingCenterY),
                    size = Size(wingW, wingH * 0.5f),
                    cornerRadius = CornerRadius(wingH * 0.5f, wingH * 0.5f)
                )
            }
        }
    }

    private fun DrawScope.drawParticles(particles: List<Particle>) {
        val count = particles.size
        for (i in 0 until count) {
            val p = particles[i]
            if (p.alpha > 0.01f) {
                drawCircle(
                    color = p.color.copy(alpha = p.alpha),
                    radius = p.size,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}
