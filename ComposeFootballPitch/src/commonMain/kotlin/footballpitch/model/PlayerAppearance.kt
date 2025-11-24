package footballpitch.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * High-level style for how a player's shirt should be rendered.
 *
 * The rendering layer chooses the concrete icon shape for each style.
 * New values can be added here without breaking existing call sites.
 */
enum class ShirtStyle {
    /** Standard outfield player shirt shape. */
    CLASSIC,

    /** Goalkeeper shirt, typically distinct from teammates. */
    GOALKEEPER,

    /**
     * Very simple circular marker, useful for minimal or tactical views
     * where shirt details are not important.
     */
    CIRCLE,

    /**
     * Shirt filled with simple stripes to improve team distinction.
     */
    STRIPED,

    /**
     * Shirt with an emphasised collar area for extra visual detail.
     */
    COLLAR,
}

/**
 * Appearance configuration for a single player icon on the pitch.
 *
 * This model is intentionally small so it can be created on the fly
 * during rendering or cached by the caller.
 */
@Immutable
data class PlayerAppearance(
    /** Base color used to render the player's shirt or marker. */
    val shirtColor: Color,
    /** High-level icon style to use when drawing this player. */
    val style: ShirtStyle = ShirtStyle.CLASSIC,
    /** Optional squad number rendered as a badge on the shirt. */
    val number: String? = null,
    /** Color used for the number badge outline. */
    val numberColor: Color = Color.White,
)
