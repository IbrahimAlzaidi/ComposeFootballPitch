package footballpitch.model

/**
 * Orientation of the pitch in the canvas.
 *
 * This affects both team positions (interpreted as normalized values) and
 * the direction of goals (left/right or top/bottom).
 */
enum class PitchOrientation {
    /** Goals on the left/right, standard TV-style orientation. */
    Horizontal,

    /** Goals at the top and bottom of the screen. */
    Vertical,

    /** Horizontal orientation mirrored 180 degrees. */
    HorizontalReversed,

    /** Vertical orientation mirrored 180 degrees. */
    VerticalReversed,
}
