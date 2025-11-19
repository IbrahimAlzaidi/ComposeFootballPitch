package footballpitch.model

import androidx.compose.runtime.Immutable

/**
 * High-level configuration for a single team in a match.
 *
 * This allows you to define the team name, colours, formation, attack
 * direction and kit style in one place and convert it to a [TeamLineup]
 * when rendering.
 */
@Immutable
data class TeamSetup(
    /** Human-readable team name (e.g. "Home" or club name). */
    val name: String,
    /** Field-player colour encoded as ARGB long (e.g. `0xFF1E88E5`). */
    val colorArgb: Long,
    /** Optional override for the goalkeeper colour. */
    val goalkeeperColorArgb: Long? = null,
    /** Tactical formation used to generate player positions. */
    val formation: Formation,
    /** Default shirt styles for field players and goalkeeper. */
    val kitStyle: TeamKitStyle = TeamKitStyle(),
    /** Direction this team is attacking (left-to-right or right-to-left). */
    val attackDirection: AttackDirection = AttackDirection.LeftToRight
)

/**
 * Pair of home and away team setups for a single match.
 */
@Immutable
data class MatchTeams(
    val home: TeamSetup,
    val away: TeamSetup
)

/**
 * Convert this [TeamSetup] to a [TeamLineup] suitable for [footballpitch.FootballPitch].
 */
fun TeamSetup.toTeamLineup(): TeamLineup =
    formation.toTeamLineup(
        teamName = name,
        colorArgb = colorArgb,
        goalkeeperColorArgb = goalkeeperColorArgb,
        kitStyle = kitStyle,
        attackDirection = attackDirection
    )

/**
 * Convert both [home] and [away] setups into [TeamLineup]s for rendering.
 */
fun MatchTeams.toLineups(): Pair<TeamLineup, TeamLineup> =
    home.toTeamLineup() to away.toTeamLineup()
