package footballpitch.model

import androidx.compose.runtime.Immutable

/**
 * Normalized position of a point on the pitch.
 *
 * Both [x] and [y] are in the range [0f, 1f], where (0f, 0f) is the
 * bottom-left corner and (1f, 1f) is the top-right corner in the
 * default horizontal orientation.
 */
@Immutable
data class PitchPosition(val x: Float, val y: Float)

/**
 * Description of a team's lineup rendered on the pitch.
 *
 * This is a lightweight, immutable model that the drawing code uses to
 * position players and colour their shirts.
 */
@Immutable
data class TeamLineup(
    /** Human-readable team label shown only in your own UI. */
    val teamName: String,
    /** Team field-player colour encoded as ARGB long (e.g. `0xFF1E88E5`). */
    val colorArgb: Long,
    /** Optional override for goalkeeper shirt colour; falls back to a derived colour when null. */
    val goalkeeperColorArgb: Long? = null,
    /** Players to render for this team, including the goalkeeper. */
    val players: List<Player>,
    /** Default shirt styles to use for field players and goalkeeper. */
    val kitStyle: TeamKitStyle = TeamKitStyle(),
)

/**
 * Information about an individual player on the pitch.
 */
@Immutable
data class Player(
    /** Normalized position of the player on the pitch. */
    val position: PitchPosition,
    /** Optional squad number to render on the shirt. */
    val number: Int? = null,
    /** Whether this player should be treated as a goalkeeper. */
    val isGoalkeeper: Boolean = false,
)
