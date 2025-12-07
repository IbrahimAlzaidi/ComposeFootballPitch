package footballpitch.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import footballpitch.PitchScaleCalculator
import footballpitch.model.PitchStyle
import footballpitch.model.PlayerAppearance
import footballpitch.model.PlayerNameStyle
import footballpitch.model.ShirtStyle
import footballpitch.model.TeamLineup

/**
 * Rendering utilities for drawing player icons (shirts, markers, numbers).
 */
internal fun DrawScope.drawTeam(
    team: TeamLineup,
    textMeasurer: TextMeasurer,
    scale: PitchScaleCalculator,
    style: PitchStyle,
) {
    // Calculate base size relative to the pitch minor dimension
    val baseSize = size.minDimension * 0.035f
    val resolvedPlayers = resolvePlayers(team, scale, baseSize)
    // Cache text layouts to avoid re-measuring on every frame
    val textLayoutCache = mutableMapOf<String, TextLayoutResult>()

    resolvedPlayers.forEach { player ->
        // Draw a subtle shadow first for depth
        if (player.appearance.style != ShirtStyle.CIRCLE) {
            drawPlayerShadow(player.center, player.baseSize)
        }

        drawShirtIcon(
            center = player.center,
            appearance = player.appearance,
            baseSize = player.baseSize,
            textMeasurer = textMeasurer,
            textLayoutCache = textLayoutCache,
            fontFamily = style.playerNumberFontFamily,
        )

        val nameStyle = style.playerNameStyle
        if (nameStyle != null && player.name != null) {
            drawPlayerName(
                center = player.center,
                baseSize = player.baseSize,
                shirtStyle = player.appearance.style,
                name = player.name,
                textMeasurer = textMeasurer,
                textLayoutCache = textLayoutCache,
                nameStyle = nameStyle,
            )
        }
    }
}

/**
 * Draws a subtle semi-transparent oval shadow underneath the player to create depth.
 */
private fun DrawScope.drawPlayerShadow(
    center: Offset,
    baseSize: Float,
) {
    val shadowColor = Color.Black.copy(alpha = 0.20f)
    // Shadow is wider and flatter than the shirt
    val shadowWidth = baseSize * 4.5f
    val shadowHeight = baseSize * 1.2f
    // Offset downwards from the center
    val shadowCenterY = center.y + baseSize * 1.9f

    drawOval(
        color = shadowColor,
        topLeft =
            Offset(
                x = center.x - shadowWidth / 2f,
                y = shadowCenterY - shadowHeight / 2f,
            ),
        size = Size(shadowWidth, shadowHeight),
    )
}

internal data class ResolvedPlayer(
    val center: Offset,
    val appearance: PlayerAppearance,
    val baseSize: Float,
    val name: String?,
)

internal fun resolvePlayers(
    team: TeamLineup,
    scale: PitchScaleCalculator,
    baseSize: Float,
): List<ResolvedPlayer> {
    val fieldPlayerColor = Color(team.colorArgb)
    // Fallback GK color contrasting with field players if not defined
    val goalkeeperColor =
        team.goalkeeperColorArgb?.let { Color(it) }
            ?: fieldPlayerColor.copy(
                red = 1f - fieldPlayerColor.red * 0.5f,
                green = 1f - fieldPlayerColor.green * 0.5f,
                blue = 1f - fieldPlayerColor.blue * 0.5f,
            )

    val kitStyle = team.kitStyle

    return team.players.map { player ->
        val center = scale.positionToCanvas(player.position)
        val isGK = player.isGoalkeeper
        val shirtStyle = if (isGK) kitStyle.goalkeeperShirtStyle else kitStyle.fieldPlayerShirtStyle

        val appearance =
            PlayerAppearance(
                shirtColor = if (isGK) goalkeeperColor else fieldPlayerColor,
                style = shirtStyle,
                number = player.number?.toString(),
                // Default number color to white; we handle contrast later with outlines
                numberColor = Color.White,
            )
        // Make goalkeepers with long sleeves a bit smaller so they stay inside the pitch
        val goalkeeperScale = 0.70f // 0.028 / 0.035 ≈ 0.8
        val playerBaseSize =
            if (isGK && shirtStyle == ShirtStyle.GOALKEEPER) {
                baseSize * goalkeeperScale
            } else {
                baseSize
            }

        ResolvedPlayer(center = center, appearance = appearance, baseSize = playerBaseSize, name = player.name)
    }
}

/**
 * Main entry point to draw a single player icon. Route to specific style implementations.
 */
private fun DrawScope.drawShirtIcon(
    center: Offset,
    appearance: PlayerAppearance,
    baseSize: Float,
    textMeasurer: TextMeasurer,
    textLayoutCache: MutableMap<String, TextLayoutResult>,
    fontFamily: FontFamily?,
) {
    if (appearance.style == ShirtStyle.CIRCLE) {
        drawCircleMarker(center, baseSize, appearance, textMeasurer, textLayoutCache, fontFamily)
        return
    }

    // Define the bounding box for the shirt based on the baseSize.
    val shirtHeight = baseSize * 3.2f
    val shirtWidth = baseSize * 3.8f
    val top = center.y - shirtHeight / 2f
    val left = center.x - shirtWidth / 2f
    val right = center.x + shirtWidth / 2f
    val bottom = top + shirtHeight

    // 1. Draw the shirt body based on style
    when (appearance.style) {
        // Classic shape with different patterns
        ShirtStyle.CLASSIC -> drawClassicShirt(left, right, top, bottom, appearance)
        ShirtStyle.STRIPED -> drawStripedShirt(left, right, top, bottom, appearance)
        ShirtStyle.COLLAR -> drawCollarShirt(left, right, top, bottom, appearance)
        // Distinct shapes
        ShirtStyle.GOALKEEPER -> drawLongSleeveShirt(left, right, top, bottom, appearance)
        else -> drawClassicShirt(left, right, top, bottom, appearance)
    }

    // 2. Draw the number on top
    if (appearance.number != null) {
        drawPlayerNumberDirectly(
            center = center,
            shirtTop = top,
            shirtHeight = shirtHeight,
            appearance = appearance,
            textMeasurer = textMeasurer,
            textLayoutCache = textLayoutCache,
            fontFamily = fontFamily,
        )
    }
}

/**
 * Draw a player name beneath the shirt. Uses a fixed font size and abbreviates long labels
 * (initials + hard character cap) instead of painting ellipsis to keep draw calls lean.
 */
@OptIn(ExperimentalTextApi::class)
internal fun DrawScope.drawPlayerName(
    center: Offset,
    baseSize: Float,
    shirtStyle: ShirtStyle,
    name: String,
    textMeasurer: TextMeasurer,
    textLayoutCache: MutableMap<String, TextLayoutResult>,
    nameStyle: PlayerNameStyle,
) {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return

    val shirtHeight = if (shirtStyle == ShirtStyle.CIRCLE) baseSize * 2.2f else baseSize * 3.2f
    val textCenterY = center.y + shirtHeight / 2f + baseSize * nameStyle.verticalPaddingFactor

    val fixedFontSizeSp =
        nameStyle.textStyle.fontSize.takeIf { it.value > 0 }
            ?: (baseSize * 1.0f / (density * fontScale)).sp

    val maxWidth = baseSize * nameStyle.maxWidthFactor
    val styleKey = "NAME_STYLE_${fixedFontSizeSp.value}_${nameStyle.textStyle.fontWeight}"
    val concreteStyle = nameStyle.textStyle.merge(TextStyle(fontSize = fixedFontSizeSp))

    fun measure(text: String): TextLayoutResult =
        textLayoutCache.getOrPut("${text}_$styleKey") {
            textMeasurer.measure(
                text = text,
                style = concreteStyle,
                softWrap = false,
                maxLines = 1,
            )
        }

    val maxChars = nameStyle.maxCharacters.coerceAtLeast(4)
    val initial = abbreviateName(trimmed, maxChars, nameStyle.useInitialsForGivenNames)
    var candidate = initial.take(maxChars)
    var layout = measure(candidate)

    while (layout.size.width > maxWidth && candidate.length > 1) {
        candidate = candidate.dropLast(1)
        layout = measure(candidate)
    }

    val strokeWidth = baseSize * 0.12f
    val topLeft =
        Offset(
            x = center.x - layout.size.width / 2f,
            y = textCenterY - layout.size.height / 2f,
        )

    drawText(
        textLayoutResult = layout,
        color = nameStyle.outlineColor,
        topLeft = topLeft,
        drawStyle = Stroke(width = strokeWidth, miter = 3f),
    )

    drawText(
        textLayoutResult = layout,
        color = nameStyle.textStyle.color,
        topLeft = topLeft,
        drawStyle = Fill,
    )
}

// ============================================================================================
// GEOMETRY BUILDERS (New distinct shapes)
// ============================================================================================

/**
 * Shape 1: The Classic, natural short-sleeve shirt with rounded shoulders and hem.
 */
private fun buildClassicShortSleevePath(
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
): Path {
    val width = right - left
    val height = bottom - top
    val centerX = (left + right) / 2f

    val neckBottom = top + height * 0.12f
    val shoulderY = top + height * 0.08f
    val sleeveEndY = top + height * 0.38f
    val armpitY = top + height * 0.45f
    val torsoInset = width * 0.18f
    val hemCurve = height * 0.05f

    return Path().apply {
        moveTo(centerX, neckBottom)
        // Soft rounded neck curve
        quadraticBezierTo(centerX + width * 0.1f, neckBottom, right - width * 0.2f, top)
        // Rounded shoulder
        quadraticBezierTo(right, top, right, shoulderY)
        // Sleeve outer edge
        lineTo(right + width * 0.05f, sleeveEndY)
        // Sleeve underarm curve to torso
        quadraticBezierTo(right - width * 0.05f, armpitY, right - torsoInset, armpitY)
        // Torso side
        lineTo(right - torsoInset, bottom - hemCurve)
        // Curved hem
        quadraticBezierTo(centerX, bottom + hemCurve, left + torsoInset, bottom - hemCurve)
        // Left side mirror
        lineTo(left + torsoInset, armpitY)
        quadraticBezierTo(left + width * 0.05f, armpitY, left - width * 0.05f, sleeveEndY)
        lineTo(left, shoulderY)
        quadraticBezierTo(left, top, left + width * 0.2f, top)
        quadraticBezierTo(centerX - width * 0.1f, neckBottom, centerX, neckBottom)
        close()
    }
}

/**
 * Shape 2: Modern, sharper athletic cut with a V-neck.
 */
private fun buildModernVNeckPath(
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
): Path {
    val width = right - left
    val height = bottom - top
    val centerX = (left + right) / 2f

    // Sharper V-neck
    val vNeckBottomY = top + height * 0.18f
    val neckWidthHalf = width * 0.15f
    val shoulderY = top + height * 0.05f
    val sleeveEndY = top + height * 0.35f
    val torsoInset = width * 0.20f // Slimmer fit

    return Path().apply {
        moveTo(centerX, vNeckBottomY)
        lineTo(centerX + neckWidthHalf, top) // Straight V line
        lineTo(right - width * 0.1f, shoulderY) // Sharper shoulder line
        lineTo(right + width * 0.02f, sleeveEndY) // Shorter, tighter sleeve
        lineTo(right - torsoInset, sleeveEndY + height * 0.05f) // Sharper armpit
        lineTo(right - torsoInset, bottom) // Straight side
        lineTo(left + torsoInset, bottom) // Straight hem
        lineTo(left + torsoInset, sleeveEndY + height * 0.05f)
        lineTo(left - width * 0.02f, sleeveEndY)
        lineTo(left + width * 0.1f, shoulderY)
        lineTo(centerX - neckWidthHalf, top)
        close()
    }
}

/**
 * Shape 3: Long Sleeves (e.g. for Goalkeepers).
 */
private fun buildLongSleevePath(
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
): Path {
    val width = right - left
    val height = bottom - top
    val centerX = (left + right) / 2f

    val neckBottom = top + height * 0.12f
    val shoulderY = top + height * 0.08f
    // Sleeves extend further down and out
    val sleeveEndX = width * 0.4f
    val sleeveEndY = top + height * 0.75f
    val wristWidth = width * 0.1f
    val armpitY = top + height * 0.45f
    val torsoInset = width * 0.18f

    return Path().apply {
        moveTo(centerX, neckBottom)
        // Standard neck/shoulder start
        quadraticBezierTo(centerX + width * 0.1f, neckBottom, right - width * 0.2f, top)
        quadraticBezierTo(right, top, right, shoulderY)

        // Long sleeve outer edge ending at wrist
        lineTo(right + sleeveEndX, sleeveEndY)
        // Wrist opening
        lineTo(right + sleeveEndX - wristWidth, sleeveEndY + wristWidth * 0.5f)
        // Long sleeve inner edge back to armpit
        lineTo(right - torsoInset + width * 0.05f, armpitY + height * 0.1f)

        // Torso down to bottom
        lineTo(right - torsoInset, bottom)
        // Simple slightly curved hem
        quadraticBezierTo(centerX, bottom + height * 0.03f, left + torsoInset, bottom)

        // Left side mirror
        lineTo(left + torsoInset - width * 0.05f, armpitY + height * 0.1f)
        lineTo(left - sleeveEndX + wristWidth, sleeveEndY + wristWidth * 0.5f)
        lineTo(left - sleeveEndX, sleeveEndY)
        lineTo(left, shoulderY)
        quadraticBezierTo(left, top, left + width * 0.2f, top)
        quadraticBezierTo(centerX - width * 0.1f, neckBottom, centerX, neckBottom)
        close()
    }
}

// ============================================================================================
// SHIRT PAINTERS (Applying color/patterns to geometry)
// ============================================================================================

/**
 * Shared helper to draw a shirt with a specific geometry and fill logic.
 * Handles the common depth gradient and outline.
 */
private fun DrawScope.drawStyledShirt(
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
    appearance: PlayerAppearance,
    pathBuilder: (Float, Float, Float, Float) -> Path,
    // Optional custom fill painter (for stripes, etc.). Defaults to standard gradient.
    customFill: (DrawScope.(Path, Color) -> Unit)? = null,
) {
    val shirtPath = pathBuilder(left, right, top, bottom)
    val width = right - left
    val height = bottom - top
    val centerX = (left + right) / 2f
    val centerY = (top + bottom) / 2f

    val mainColor = appearance.shirtColor
    // Create a darker variant for shading edges and outline
    val darkerColor =
        mainColor.copy(
            red = mainColor.red * 0.75f,
            green = mainColor.green * 0.75f,
            blue = mainColor.blue * 0.75f,
        )

    // 1. Fill
    if (customFill != null) {
        customFill(shirtPath, mainColor)
    } else {
        // Standard Radial Gradient for depth
        val shiftGradientBrush =
            Brush.radialGradient(
                colors = listOf(mainColor, darkerColor),
                center = Offset(centerX, centerY - height * 0.15f),
                radius = width * 0.85f,
            )
        drawPath(shirtPath, brush = shiftGradientBrush, style = Fill)
    }

    // 2. Outline
    val outlineColor = darkerColor.copy(alpha = 0.9f)
    drawPath(shirtPath, color = outlineColor, style = Stroke(width = width * 0.015f))
}

private fun DrawScope.drawClassicShirt(
    l: Float,
    r: Float,
    t: Float,
    b: Float,
    app: PlayerAppearance,
) {
    drawStyledShirt(l, r, t, b, app, ::buildClassicShortSleevePath)
}

// Example of how you might map a "MODERN" style if you added it to the enum later
private fun DrawScope.drawModernVNeckShirt(
    l: Float,
    r: Float,
    t: Float,
    b: Float,
    app: PlayerAppearance,
) {
    drawStyledShirt(l, r, t, b, app, ::buildModernVNeckPath)
}

private fun DrawScope.drawLongSleeveShirt(
    l: Float,
    r: Float,
    t: Float,
    b: Float,
    app: PlayerAppearance,
) {
    drawStyledShirt(l, r, t, b, app, ::buildLongSleevePath)
}

private fun DrawScope.drawStripedShirt(
    l: Float,
    r: Float,
    t: Float,
    b: Float,
    app: PlayerAppearance,
) {
    // Use Classic shape, but provide custom fill logic for stripes
    drawStyledShirt(l, r, t, b, app, ::buildClassicShortSleevePath) { path, primary ->
        val secondary = if (primary.isLightColor()) primary.darken(0.2f) else primary.lighten(0.2f)
        val stripes = 7
        val stripeWidth = (r - l) / stripes

        clipPath(path) {
            // Draw base color first
            drawRect(color = primary, topLeft = Offset(l, t), size = Size(r - l, b - t))
            // Draw stripes over it
            repeat(stripes / 2 + 1) { i ->
                drawRect(
                    color = secondary,
                    topLeft = Offset(l + (i * 2 * stripeWidth), t),
                    size = Size(stripeWidth, b - t),
                )
            }
        }
    }
}

private fun DrawScope.drawCollarShirt(
    l: Float,
    r: Float,
    t: Float,
    b: Float,
    app: PlayerAppearance,
) {
    // 1. Draw base shirt first
    drawClassicShirt(l, r, t, b, app)

    // 2. Draw simple white collar overlay
    val width = r - l
    val height = b - t
    val centerX = (l + r) / 2f
    val collarTop = t + height * 0.05f
    val collarBottom = t + height * 0.22f
    val collarWidth = width * 0.25f

    val collarPath =
        Path().apply {
            moveTo(centerX, collarBottom)
            lineTo(centerX + collarWidth / 2, collarTop)
            // Small fold detail
            lineTo(centerX + collarWidth / 2 - width * 0.02f, collarTop + height * 0.05f)
            lineTo(centerX - collarWidth / 2 + width * 0.02f, collarTop + height * 0.05f)
            lineTo(centerX - collarWidth / 2, collarTop)
            close()
        }
    // Draw collar with slight shadow
    drawPath(collarPath, Color.White, style = Fill)
    drawPath(collarPath, Color.Black.copy(alpha = 0.2f), style = Stroke(width * 0.005f))
}

// ============================================================================================
// TYPOGRAPHY & MARKERS (Professional Number Rendering)
// ============================================================================================

/**
 * Draws the player number directly onto the shirt fabric using a professional
 * outline+fill technique to ensure contrast on any background.
 */
@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawPlayerNumberDirectly(
    center: Offset,
    shirtTop: Float,
    shirtHeight: Float,
    appearance: PlayerAppearance,
    textMeasurer: TextMeasurer,
    textLayoutCache: MutableMap<String, TextLayoutResult>,
    fontFamily: FontFamily?,
) {
    val number = appearance.number ?: return

    // Position number in the upper-middle of the shirt back
    val numberCenterY = shirtTop + shirtHeight * 0.45f
    // Size relative to shirt height
    val fontSizeSp = (shirtHeight * 0.45f / (density * fontScale)).sp

    // 1. Define colors for contrast
    val isShirtLight = appearance.shirtColor.isLightColor() || appearance.style == ShirtStyle.STRIPED
    // If shirt is light/busy, use dark fill with light outline.
    // If shirt is dark, use light fill with dark outline.
    val fillTextColor = if (isShirtLight) Color.Black.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.95f)
    val outlineTextColor = if (isShirtLight) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.6f)

    val baseTextStyle =
        TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontSize = fontSizeSp,
        )

    // 2. Measure text once
    val textLayout =
        textLayoutCache.getOrPut("${number}_${fontSizeSp}_$fontFamily") {
            textMeasurer.measure(text = number, style = baseTextStyle)
        }

    val textTopLeft =
        Offset(
            x = center.x - textLayout.size.width / 2f,
            y = numberCenterY - textLayout.size.height / 2f,
        )

    // 3. Draw Outline Stroke first (thick, contrasting color)
    // The stroke width needs to scale with the font size
    val strokeWidth = shirtHeight * 0.025f
    drawText(
        textLayoutResult = textLayout,
        color = outlineTextColor,
        topLeft = textTopLeft,
        drawStyle = Stroke(width = strokeWidth, miter = 4f),
    )

    // 4. Draw Solid Fill second (main number color)
    drawText(
        textLayoutResult = textLayout,
        color = fillTextColor,
        topLeft = textTopLeft,
        drawStyle = Fill,
    )
}

/**
 * Simple circular marker style (fallback for tactical views).
 */
@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawCircleMarker(
    center: Offset,
    baseSize: Float,
    appearance: PlayerAppearance,
    textMeasurer: TextMeasurer,
    textLayoutCache: MutableMap<String, TextLayoutResult>,
    fontFamily: FontFamily?,
) {
    val radius = baseSize * 1.4f
    val mainColor = appearance.shirtColor
    val darkerColor = mainColor.darken(0.3f)

    // Gradient fill for the marker
    val brush =
        Brush.radialGradient(
            colors = listOf(mainColor, darkerColor),
            center = center,
            radius = radius * 1.2f,
        )
    drawCircle(brush = brush, center = center, radius = radius, style = Fill)
    drawCircle(color = darkerColor.copy(alpha = 0.8f), center = center, radius = radius, style = Stroke(width = baseSize * 0.1f))

    // Draw simple number inside if present
    appearance.number?.let { number ->
        val textColor = if (mainColor.isLightColor()) Color.Black else Color.White
        val textLayout =
            textLayoutCache.getOrPut("C_${number}_$radius") {
                textMeasurer.measure(
                    text = number,
                    style =
                        TextStyle(
                            color = textColor,
                            fontSize = (radius * 1.0f / (density * fontScale)).sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontFamily = fontFamily,
                        ),
                )
            }
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(center.x - textLayout.size.width / 2f, center.y - textLayout.size.height / 2f),
        )
    }
}

// ============================================================================================
// UTILS
// ============================================================================================

private fun Color.isLightColor(): Boolean {
    // Calculate luminance to determine if color is "light"
    val luminance = 0.299f * red + 0.587f * green + 0.114f * blue
    return luminance > 0.6f
}

private fun Color.darken(factor: Float): Color {
    return copy(
        red = (red * (1f - factor)).coerceIn(0f, 1f),
        green = (green * (1f - factor)).coerceIn(0f, 1f),
        blue = (blue * (1f - factor)).coerceIn(0f, 1f),
    )
}

private fun Color.lighten(factor: Float): Color {
    return copy(
        red = (red + factor).coerceAtMost(1f),
        green = (green + factor).coerceAtMost(1f),
        blue = (blue + factor).coerceAtMost(1f),
    )
}

/**
 * Abbreviate a name by turning given names into initials and enforcing a hard character cap.
 * This keeps labels visually consistent without expensive per-character layout attempts.
 */
private fun abbreviateName(
    name: String,
    maxChars: Int,
    useInitialsForGivenNames: Boolean,
): String {
    val parts =
        name.split(" ", "-", "\u00A0")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    if (parts.isEmpty()) return ""
    if (!useInitialsForGivenNames || parts.size == 1) return parts.joinToString(" ").take(maxChars)

    val last = parts.last()
    val initials =
        parts
            .dropLast(1)
            .joinToString(" ") { "${it.first().uppercase()}." }
            .trim()

    val combined = listOfNotNull(initials.takeIf { it.isNotEmpty() }, last).joinToString(" ").trim()
    if (combined.length <= maxChars) return combined

    val availableForLast = (maxChars - initials.length - if (initials.isNotEmpty()) 1 else 0).coerceAtLeast(3)
    val shortenedLast = last.take(availableForLast)
    return (if (initials.isNotEmpty()) "$initials $shortenedLast" else shortenedLast).take(maxChars)
}
