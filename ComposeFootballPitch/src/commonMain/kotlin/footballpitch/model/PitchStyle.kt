package footballpitch.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

private val DefaultGrassColors =
    listOf(
        Color(0xFF166C31),
        Color(0xFF0E5A26),
    )

/**
 * Orientation of stripe backgrounds when [PitchBackground.Stripes] is used.
 */
enum class StripeOrientation {
    /** Stripes run parallel to the touchlines. */
    Vertical,

    /** Stripes run parallel to the goal-lines. */
    Horizontal,
}

/**
 * Direction of the linear gradient when [PitchBackground.Gradient] is used.
 */
enum class GradientDirection {
    /** Gradient runs from top to bottom. */
    Vertical,

    /** Gradient runs from left to right. */
    Horizontal,

    /** Gradient runs from top-left to bottom-right. */
    Diagonal,
}

/**
 * Strategy for drawing the grass/background of the pitch.
 *
 * New background types can be added here without changing the public
 * [footballpitch.FootballPitch] composable signature.
 */
@Immutable
sealed interface PitchBackground {
    /**
     * Simple, solid-colour background (no stripes or patterns).
     */
    @Immutable
    data class Solid(
        val color: Color,
    ) : PitchBackground

    /**
     * Alternating stripes using the provided [colors].
     *
     * The colors repeat if [stripeCount] is larger than the list size.
     */
    @Immutable
    data class Stripes(
        val colors: List<Color> = DefaultGrassColors,
        val stripeCount: Int = 8,
        val orientation: StripeOrientation = StripeOrientation.Vertical,
    ) : PitchBackground

    /**
     * Checkerboard pattern, typically used for stylized or training pitches.
     */
    @Immutable
    data class Checkerboard(
        val colors: List<Color> = DefaultGrassColors,
        val rows: Int = 8,
        val columns: Int = 8,
    ) : PitchBackground

    /**
     * Multi-stop linear gradient background.
     */
    @Immutable
    data class Gradient(
        val colors: List<Color> = DefaultGrassColors,
        val direction: GradientDirection = GradientDirection.Vertical,
    ) : PitchBackground
}

/**
 * High-level styling configuration for how the pitch is rendered.
 *
 * This controls the grass/background, line colour and line thickness,
 * independent of the logical pitch dimensions or teams.
 */
@Immutable
data class PitchStyle(
    /** Background strategy to use for the grass. */
    val background: PitchBackground = PitchBackground.Stripes(),
    /** Color used for all pitch markings (lines, circles, arcs). */
    val lineColor: Color = Color.White,
    /**
     * Multiplier applied on top of the default, dimension-based line width.
     *
     * Values greater than 1f make lines thicker; values between 0f and 1f
     * make them thinner. Negative values are clamped to 0f.
     */
    val lineThicknessFactor: Float = 1f,
    /** Optional custom font family for player jersey numbers. */
    val playerNumberFontFamily: FontFamily? = null,
    /**
     * Optional styling for player names rendered beneath shirts. When null, names are skipped
     * entirely for better performance and a cleaner tactical view.
     */
    val playerNameStyle: PlayerNameStyle? = null,
)

@Immutable
data class PlayerNameStyle(
    /**
     * Base text style for names; stays fixed for consistency unless caller changes it.
     * Keep sizes modest to avoid overlap with nearby players.
     */
    val textStyle: TextStyle =
        TextStyle(
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            fontSize = 8.sp,
        ),
    /** Outline color behind the text for contrast. */
    val outlineColor: Color = Color.Black.copy(alpha = 0.6f),
    /** Maximum width allowed for a name, relative to base icon size. */
    val maxWidthFactor: Float = 4.6f,
    /**
     * Maximum characters to render; longer names are abbreviated to this count without ellipsis
     * to keep draw calls fast and predictable.
     */
    val maxCharacters: Int = 12,
    /**
     * Whether to compress given names to initials (e.g., "K. Mbappe") before truncating to [maxCharacters].
     * Useful for mixed-length squads where you want consistent visual width without per-name tuning.
     */
    val useInitialsForGivenNames: Boolean = true,
    /** Vertical padding (in baseSize multiples) between shirt bottom and name center. */
    val verticalPaddingFactor: Float = 0.7f,
)
