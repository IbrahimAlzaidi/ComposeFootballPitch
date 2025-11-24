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
    /**
     * Direction this team is attacking (left-to-right or right-to-left).
     *
     * When using [MatchTeams.toLineups] this is auto-mirrored to keep teams
     * facing each other, so callers usually do not need to set it manually.
     */
    val attackDirection: AttackDirection = AttackDirection.LeftToRight,
)

/**
 * Pair of home and away team setups for a single match.
 */
@Immutable
data class MatchTeams(
    val home: TeamSetup,
    val away: TeamSetup,
)

/**
 * Convert this [TeamSetup] to a [TeamLineup] suitable for [footballpitch.FootballPitch].
 *
 * @param resolvedAttackDirection Direction to render the lineup; defaults to the setup value
 * but can be overridden (e.g. when auto-mirroring home/away teams).
 */
fun TeamSetup.toTeamLineup(resolvedAttackDirection: AttackDirection = attackDirection): TeamLineup =
    formation.toTeamLineup(
        teamName = name,
        colorArgb = colorArgb,
        goalkeeperColorArgb = goalkeeperColorArgb,
        kitStyle = kitStyle,
        attackDirection = resolvedAttackDirection,
    )

/**
 * Convert both [home] and [away] setups into [TeamLineup]s for rendering, automatically
 * mirroring the away side so that teams face each other without overlapping formations.
 */
fun MatchTeams.toLineups(): Pair<TeamLineup, TeamLineup> {
    val homeDirection = home.attackDirection
    val awayDirection =
        when (away.attackDirection) {
            homeDirection -> homeDirection.opposite()
            else -> away.attackDirection
        }

    return home.toTeamLineup(homeDirection) to away.toTeamLineup(awayDirection)
}
