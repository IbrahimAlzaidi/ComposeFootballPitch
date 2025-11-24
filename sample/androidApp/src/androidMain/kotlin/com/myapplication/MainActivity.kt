package com.myapplication

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import footballpitch.FootballPitch
import footballpitch.model.Formations
import footballpitch.model.MatchTeams
import footballpitch.model.PitchBackground
import footballpitch.model.PitchStyle
import footballpitch.model.ShirtStyle
import footballpitch.model.TeamKitStyle
import footballpitch.model.TeamSetup
import footballpitch.model.toLineups

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val matchTeams = MatchTeams(
                home = TeamSetup(
                    name = "Home",
                    colorArgb = 0xFF1E88E5, // blue
                    goalkeeperColorArgb = 0xFFFFC107,
                    formation = Formations.fourFourTwo(),
                    kitStyle = TeamKitStyle(
                        fieldPlayerShirtStyle = ShirtStyle.STRIPED,
                        goalkeeperShirtStyle = ShirtStyle.GOALKEEPER
                    )
                ),
                away = TeamSetup(
                    name = "Away",
                    colorArgb = 0xFFEF5350, // red
                    goalkeeperColorArgb = 0xFF8D6E63,
                    formation = Formations.threeFourThree(),
                    kitStyle = TeamKitStyle(
                        fieldPlayerShirtStyle = ShirtStyle.COLLAR,
                        goalkeeperShirtStyle = ShirtStyle.GOALKEEPER
                    )
                )
            )

            val (homeLineup, awayLineup) = matchTeams.toLineups()

            FootballPitch(
                style = PitchStyle(
                    background = PitchBackground.Stripes()
                ),
                homeTeam = homeLineup,
                awayTeam = awayLineup
            )
        }
    }
}
