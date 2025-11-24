package footballpitch.model

/**
 * Real-world pitch dimensions expressed in meters.
 *
 * Defaults used by [footballpitch.FootballPitch] match the typical
 * FIFA 105m x 68m configuration, but you can override them to display
 * other pitch sizes (training grounds, futsal, etc.).
 */
data class PitchDimensions(
    val length: Float,
    val width: Float,
    val penaltyAreaDepth: Float,
    val penaltyAreaWidth: Float,
    val goalAreaDepth: Float,
    val goalAreaWidth: Float,
    val penaltyMarkDistance: Float,
    val circleRadius: Float,
    val cornerArcRadius: Float,
)
