package footballpitch.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

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
)
