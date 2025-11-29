package footballpitch.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import footballpitch.PitchScaleCalculator
import footballpitch.model.PlayerAppearance
import footballpitch.model.ShirtStyle
import footballpitch.model.TeamLineup
import kotlin.math.PI
import kotlin.math.tan

/**
 * Rendering utilities for drawing player icons (shirts, markers, numbers).
 * Draw the whole team using simple, stylised player icons.
 */
internal fun DrawScope.drawTeam(
    team: TeamLineup,
    textMeasurer: TextMeasurer,
    scale: PitchScaleCalculator,
) {
    val baseSize = size.minDimension * 0.035f
    val resolvedPlayers = resolvePlayers(team, scale, baseSize)
    val textLayoutCache = mutableMapOf<String, TextLayoutResult>()
    resolvedPlayers.forEach { player ->
        drawShirtIcon(player.center, player.appearance, player.baseSize, textMeasurer, textLayoutCache)
    }
}

internal data class ResolvedPlayer(
    val center: Offset,
    val appearance: PlayerAppearance,
    val baseSize: Float,
)

internal fun resolvePlayers(
    team: TeamLineup,
    scale: PitchScaleCalculator,
    baseSize: Float,
): List<ResolvedPlayer> {
    // Compute once, not per player
    val fieldPlayerColor = Color(team.colorArgb)
    val goalkeeperColor =
        team.goalkeeperColorArgb?.let { Color(it) }
            // simple fallback: a contrasting variant
            ?: fieldPlayerColor.copy(
                red = 1f - fieldPlayerColor.red * 0.5f,
                green = 1f - fieldPlayerColor.green * 0.5f,
                blue = 1f - fieldPlayerColor.blue * 0.5f,
            )

    val kitStyle = team.kitStyle

    return team.players.map { player ->
        val center = scale.positionToCanvas(player.position)

        val isGK = player.isGoalkeeper
        val shirtStyle =
            if (isGK) {
                kitStyle.goalkeeperShirtStyle
            } else {
                kitStyle.fieldPlayerShirtStyle
            }

        val appearance =
            PlayerAppearance(
                shirtColor = if (isGK) goalkeeperColor else fieldPlayerColor,
                style = shirtStyle,
                number = player.number?.toString(),
            )

        ResolvedPlayer(center = center, appearance = appearance, baseSize = baseSize)
    }
}

/**
 * Draw a single player icon at [center] using the configured [appearance].
 */
private fun DrawScope.drawShirtIcon(
    center: Offset,
    appearance: PlayerAppearance,
    baseSize: Float,
    textMeasurer: TextMeasurer,
    textLayoutCache: MutableMap<String, TextLayoutResult>,
) {
    when (appearance.style) {
        ShirtStyle.CIRCLE -> {
            drawCircleMarker(center, baseSize, appearance)
            if (appearance.number != null) {
                drawCircleNumber(
                    center = center,
                    baseSize = baseSize,
                    appearance = appearance,
                    textMeasurer = textMeasurer,
                    textLayoutCache = textLayoutCache,
                )
            }
        }

        ShirtStyle.CLASSIC,
        ShirtStyle.GOALKEEPER,
        ShirtStyle.STRIPED,
        ShirtStyle.COLLAR,
        -> {
            val shirtHeight = baseSize * 3.2f
            val shirtWidth = baseSize * 3.8f

            val top = center.y - shirtHeight / 2f
            val left = center.x - shirtWidth / 2f
            val right = center.x + shirtWidth / 2f
            val bottom = top + shirtHeight

            when (appearance.style) {
                ShirtStyle.CLASSIC -> drawClassicShirt(left, right, top, bottom, appearance)
                ShirtStyle.GOALKEEPER -> drawGoalkeeperShirt(left, right, top, bottom, appearance)
                ShirtStyle.STRIPED -> drawStripedShirt(left, right, top, bottom, appearance)
                ShirtStyle.COLLAR -> drawCollarShirt(left, right, top, bottom, appearance)
                ShirtStyle.CIRCLE -> Unit // already handled
            }

            if (appearance.number != null) {
                drawPlayerNumber(
                    center = center,
                    top = top,
                    height = shirtHeight,
                    appearance = appearance,
                    textMeasurer = textMeasurer,
                    textLayoutCache = textLayoutCache,
                )
            }
        }
    }
}

/**
 * Build the base classic shirt path with straight sleeves and a V-neck.
 */
private fun buildClassicShirtPath(
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
    sleeveLength: Float = 0.17f,
    sleeveWidth: Float = 0.24f,
    sleeveAngle: Float = 25f,
): Path {
    val shirtWidth = right - left
    val shirtHeight = bottom - top
    val centerX = (left + right) / 2f

    // Shoulder line position
    val yShoulder = top + shirtHeight * 0.22f
    // Where sleeve joins torso vertically
    val yUnderSleeve = top + shirtHeight * (0.22f + sleeveLength)
    val deltaY = yUnderSleeve - yShoulder

    // Convert desired angle to horizontal inset so that:
    // tan(angle) = deltaY / deltaX  ->  deltaX = deltaY / tan(angle)
    val angleRad = (sleeveAngle * (PI / 180.0)).toFloat()
    val maxInsetByWidth = shirtWidth * sleeveWidth
    val insetFromAngle = if (angleRad == 0f) maxInsetByWidth else deltaY / tan(angleRad)

    // Final inset clamped to a reasonable range
    val bodyInset =
        insetFromAngle
            .coerceAtMost(maxInsetByWidth)
            .coerceIn(shirtWidth * 0.15f, shirtWidth * 0.45f)

    val xBodyLeft = left + bodyInset
    val xBodyRight = right - bodyInset

    // --- V-neck geometry (inside top edge) ---
    val vWidth = shirtWidth * 0.22f
    val vDepth = shirtHeight * 0.11f

    val vLeftTop = centerX - vWidth / 2f
    val vRightTop = centerX + vWidth / 2f
    val vBottom = yShoulder + vDepth

    return Path().apply {
        // Top edge with V-neck
        moveTo(left, yShoulder) // outer left shoulder
        lineTo(xBodyLeft, yShoulder) // inner left shoulder
        lineTo(vLeftTop, yShoulder) // start of V
        lineTo(centerX, vBottom) // bottom of V
        lineTo(vRightTop, yShoulder) // end of V
        lineTo(xBodyRight, yShoulder) // inner right shoulder
        lineTo(right, yShoulder) // outer right shoulder

        // Right sleeve diagonal down into torso
        lineTo(xBodyRight, yUnderSleeve)

        // Torso
        lineTo(xBodyRight, bottom)
        lineTo(xBodyLeft, bottom)
        lineTo(xBodyLeft, yUnderSleeve)

        // Left sleeve diagonal back up to outer shoulder
        lineTo(left, yShoulder)

        close()
    }
}

/**
 * Classic shirt fill using the base path.
 */
private fun DrawScope.drawClassicShirt(
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
    appearance: PlayerAppearance,
) {
    val shirtPath = buildClassicShirtPath(left, right, top, bottom)
    drawPath(shirtPath, color = appearance.shirtColor, style = Fill)
    drawPath(shirtPath, color = Color.Black, style = Stroke(width = 1f))
}

/**
 * Goalkeeper shirt variant. Currently uses the same geometry as classic,
 * but split into a separate function for future customization.
 */
private fun DrawScope.drawGoalkeeperShirt(
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
    appearance: PlayerAppearance,
) {
    drawClassicShirt(left, right, top, bottom, appearance)
}

/**
 * Classic shirt filled with simple vertical stripes.
 */
private fun DrawScope.drawStripedShirt(
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
    appearance: PlayerAppearance,
) {
    val shirtPath = buildClassicShirtPath(left, right, top, bottom)
    val width = right - left
    val height = bottom - top

    val primary = appearance.shirtColor
    val secondary =
        if (primary.isLightColor()) {
            primary.copy(
                red = (primary.red * 0.8f).coerceIn(0f, 1f),
                green = (primary.green * 0.8f).coerceIn(0f, 1f),
                blue = (primary.blue * 0.8f).coerceIn(0f, 1f),
            )
        } else {
            primary.copy(
                red = (primary.red * 1.2f).coerceIn(0f, 1f),
                green = (primary.green * 1.2f).coerceIn(0f, 1f),
                blue = (primary.blue * 1.2f).coerceIn(0f, 1f),
            )
        }

    val stripes = 6
    val stripeWidth = width / stripes

    clipPath(shirtPath) {
        repeat(stripes) { index ->
            val color = if (index % 2 == 0) primary else secondary
            drawRect(
                color = color,
                topLeft = Offset(x = left + stripeWidth * index, y = top),
                size = Size(stripeWidth, height),
            )
        }
    }

    // Outline on top of stripes
    drawPath(shirtPath, color = Color.Black, style = Stroke(width = 1f))
}

/**
 * Classic shirt with an emphasised collar area.
 */
private fun DrawScope.drawCollarShirt(
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
    appearance: PlayerAppearance,
) {
    val shirtPath = buildClassicShirtPath(left, right, top, bottom)
    drawPath(shirtPath, color = appearance.shirtColor, style = Fill)

    val shirtWidth = right - left
    val shirtHeight = bottom - top
    val centerX = (left + right) / 2f

    val collarWidth = shirtWidth * 0.3f
    val collarHeight = shirtHeight * 0.16f
    val collarTop = top + shirtHeight * 0.22f
    val collarBottom = collarTop + collarHeight

    val collarPath =
        Path().apply {
            moveTo(centerX - collarWidth / 2f, collarTop)
            lineTo(centerX + collarWidth / 2f, collarTop)
            lineTo(centerX + collarWidth / 4f, collarBottom)
            lineTo(centerX - collarWidth / 4f, collarBottom)
            close()
        }

    val collarColor =
        if (appearance.shirtColor.isLightColor()) {
            appearance.shirtColor.copy(alpha = 0.9f)
        } else {
            appearance.shirtColor.copy(alpha = 0.9f)
        }

    drawPath(collarPath, color = collarColor, style = Fill)
    drawPath(shirtPath, color = Color.Black, style = Stroke(width = 1f))
}

/**
 * Simple circular marker style.
 */
private fun DrawScope.drawCircleMarker(
    center: Offset,
    baseSize: Float,
    appearance: PlayerAppearance,
) {
    val radius = baseSize * 1.6f

    drawCircle(
        color = appearance.shirtColor,
        center = center,
        radius = radius,
        style = Fill,
    )
    drawCircle(
        color = Color.Black,
        center = center,
        radius = radius,
        style = Stroke(width = 1f),
    )
}

/**
 * Draw the shirt number badge for jersey-based icons.
 */
@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawPlayerNumber(
    center: Offset,
    top: Float,
    height: Float,
    appearance: PlayerAppearance,
    textMeasurer: TextMeasurer,
    textLayoutCache: MutableMap<String, TextLayoutResult>,
) {
    val badgeRadius = height * 0.18f
    val badgeCenterY = top + height * 0.6f
    drawNumberBadge(
        center = Offset(center.x, badgeCenterY),
        radius = badgeRadius,
        appearance = appearance,
        textMeasurer = textMeasurer,
        textLayoutCache = textLayoutCache,
    )
}

/**
 * Draw the number badge for a circular marker style.
 */
@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawCircleNumber(
    center: Offset,
    baseSize: Float,
    appearance: PlayerAppearance,
    textMeasurer: TextMeasurer,
    textLayoutCache: MutableMap<String, TextLayoutResult>,
) {
    val badgeRadius = baseSize * 0.9f
    drawNumberBadge(
        center = center,
        radius = badgeRadius,
        appearance = appearance,
        textMeasurer = textMeasurer,
        textLayoutCache = textLayoutCache,
    )
}

/**
 * Shared badge painter that renders a filled circle, outline, and centred number text.
 */
@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawNumberBadge(
    center: Offset,
    radius: Float,
    appearance: PlayerAppearance,
    textMeasurer: TextMeasurer,
    textLayoutCache: MutableMap<String, TextLayoutResult>,
) {
    val number = appearance.number ?: return
    val badgeFill =
        if (appearance.shirtColor.isLightColor()) {
            Color.Black.copy(alpha = 0.55f)
        } else {
            Color.White.copy(alpha = 0.72f)
        }
    val strokeColor = appearance.numberColor
    val textColor = if (appearance.shirtColor.isLightColor()) Color.White else Color.Black

    drawCircle(color = badgeFill, center = center, radius = radius, style = Fill)
    drawCircle(color = strokeColor, center = center, radius = radius, style = Stroke(width = radius * 0.16f))

    val textLayout =
        textLayoutCache.getOrPut("${number}_${radius}_${textColor.value}") {
            textMeasurer.measureNumber(
                text = number,
                style =
                    TextStyle(
                        color = textColor,
                        fontSize = (radius * 1.15f / (density * fontScale)).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
            )
        }

    drawText(
        textLayoutResult = textLayout,
        topLeft =
            Offset(
                x = center.x - textLayout.size.width / 2f,
                y = center.y - textLayout.size.height / 2f,
            ),
    )
}

/**
 * Quick luminance check for stripe contrast.
 */
private fun Color.isLightColor(): Boolean {
    val luminance = 0.299f * red + 0.587f * green + 0.114f * blue
    return luminance > 0.5f
}

@OptIn(ExperimentalTextApi::class)
private fun TextMeasurer.measureNumber(
    text: String,
    style: TextStyle,
) = measure(
    text = text,
    style = style,
)
