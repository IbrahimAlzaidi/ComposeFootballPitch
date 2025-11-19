package footballpitch.model

import androidx.compose.runtime.Immutable

/**
 * Default shirt styles to apply for a team's players.
 *
 * This allows you to configure, for example, a striped home team and a
 * collar-style away team without touching the low-level rendering code.
 */
@Immutable
data class TeamKitStyle(
    /** Shirt style used for all outfield players (non-goalkeepers). */
    val fieldPlayerShirtStyle: ShirtStyle = ShirtStyle.CLASSIC,
    /** Shirt style used for the goalkeeper. */
    val goalkeeperShirtStyle: ShirtStyle = ShirtStyle.GOALKEEPER
)

