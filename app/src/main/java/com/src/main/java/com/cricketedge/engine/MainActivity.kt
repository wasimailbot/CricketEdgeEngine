package com.cricketedge.engine

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var team1Spinner: Spinner
    private lateinit var team2Spinner: Spinner
    private lateinit var venueSpinner: Spinner
    private lateinit var homeTeamSpinner: Spinner
    private lateinit var tossWinnerSpinner: Spinner
    private lateinit var tossDecisionSpinner: Spinner
    private lateinit var dewSpinner: Spinner
    private lateinit var rainSpinner: Spinner
    private lateinit var pitchSpinner: Spinner
    private lateinit var umpire1Spinner: Spinner
    private lateinit var umpire2Spinner: Spinner
    private lateinit var calculateButton: Button
    private lateinit var resultText: TextView

    private val teams = arrayOf(
        "Royal Challengers Bengaluru",
        "Chennai Super Kings",
        "Mumbai Indians",
        "Kolkata Knight Riders",
        "Rajasthan Royals",
        "Sunrisers Hyderabad",
        "Delhi Capitals",
        "Punjab Kings",
        "Lucknow Super Giants",
        "Gujarat Titans"
    )

    private val venues = arrayOf(
        "Select Venue",
        "M. Chinnaswamy Stadium",
        "MA Chidambaram Stadium",
        "Wankhede Stadium",
        "Eden Gardens",
        "Narendra Modi Stadium",
        "Rajiv Gandhi International Stadium",
        "Arun Jaitley Stadium",
        "Sawai Mansingh Stadium",
        "Other / Enter Later"
    )

    private val pitchTypes = arrayOf(
        "Select Pitch",
        "Batting Friendly",
        "Bowling Friendly",
        "Pace Friendly",
        "Spin Friendly",
        "Balanced",
        "Unknown"
    )

    private val dewOptions = arrayOf(
        "Unknown",
        "Low",
        "Medium",
        "High"
    )

    private val rainOptions = arrayOf(
        "No Rain Expected",
        "Low Rain Probability",
        "Medium Rain Probability",
        "High Rain Probability"
    )

    private val tossDecisions = arrayOf(
        "Not Known",
        "Bat First",
        "Field First"
    )

    private val umpires = arrayOf(
        "Not Selected",
        "Umpire 1",
        "Umpire 2",
        "Umpire 3",
        "Umpire 4"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(32, 32, 32, 48)

        scrollView.addView(root)

        addTitle(root, "CRICKET EDGE ENGINE")
        addSubtitle(
            root,
            "Real Cricket Match Probability Engine"
        )

        addSection(root, "MATCH")

        team1Spinner = addSpinner(
            root,
            "Team 1",
            teams
        )

        team2Spinner = addSpinner(
            root,
            "Team 2",
            teams
        )

        homeTeamSpinner = addSpinner(
            root,
            "Home Team",
            arrayOf("Not Selected") + teams
        )

        addSection(root, "VENUE & PITCH")

        venueSpinner = addSpinner(
            root,
            "Venue",
            venues
        )

        pitchSpinner = addSpinner(
            root,
            "Pitch Characteristics",
            pitchTypes
        )

        addSection(root, "MATCH CONDITIONS")

        addLabel(root, "Temperature (°C)")
        val temperature = EditText(this)
        temperature.hint = "Example: 28"
        temperature.inputType = 2
        root.addView(
            temperature,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addLabel(root, "Humidity (%)")
        val humidity = EditText(this)
        humidity.hint = "Example: 65"
        humidity.inputType = 2
        root.addView(
            humidity,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addLabel(root, "Wind Speed (km/h)")
        val wind = EditText(this)
        wind.hint = "Example: 12"
        wind.inputType = 2
        root.addView(
            wind,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        dewSpinner = addSpinner(
            root,
            "Dew",
            dewOptions
        )

        rainSpinner = addSpinner(
            root,
            "Rain / Weather Risk",
            rainOptions
        )

        addSection(root, "TOSS")

        tossWinnerSpinner = addSpinner(
            root,
            "Toss Winner",
            arrayOf("Not Known") + teams
        )

        tossDecisionSpinner = addSpinner(
            root,
            "Toss Decision",
            tossDecisions
        )

        addSection(root, "UMPIRES")

        umpire1Spinner = addSpinner(
            root,
            "Umpire 1",
            umpires
        )

        umpire2Spinner = addSpinner(
            root,
            "Umpire 2",
            umpires
        )

        addSection(root, "PLAYING XI")

        val xiInfo = TextView(this)
        xiInfo.text =
            "Playing XI selection will be connected to the real player database in the next stage.\n\n" +
            "Each team will eventually require exactly 11 selected players."
        xiInfo.textSize = 16f
        xiInfo.setPadding(0, 8, 0, 16)
        root.addView(xiInfo)

        val xiButton = Button(this)
        xiButton.text = "SELECT PLAYING XI"
        xiButton.setOnClickListener {
            Toast.makeText(
                this,
                "Playing XI database is the next stage.",
                Toast.LENGTH_SHORT
            ).show()
        }
        root.addView(xiButton)

        addSection(root, "PROBABILITY ENGINE")

        calculateButton = Button(this)
        calculateButton.text = "CALCULATE WIN PROBABILITY"
        calculateButton.textSize = 17f

        calculateButton.setOnClickListener {
            showTemporaryResult(
                team1Spinner.selectedItem.toString(),
                team2Spinner.selectedItem.toString()
            )
        }

        root.addView(
            calculateButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        resultText = TextView(this)
        resultText.text =
            "Probability will appear here after the real statistical engine is connected."
        resultText.textSize = 17f
        resultText.setPadding(0, 24, 0, 20)

        root.addView(resultText)

        addSection(root, "MODEL FACTORS")

        val factors = TextView(this)

        factors.text =
            "The final engine will evaluate:\n\n" +
            "• Team strength\n" +
            "• Recent team form\n" +
            "• Playing XI strength\n" +
            "• Individual batting strength\n" +
            "• Individual bowling strength\n" +
            "• All-rounder contribution\n" +
            "• Batting depth\n" +
            "• Powerplay performance\n" +
            "• Middle-over performance\n" +
            "• Death-over performance\n" +
            "• Pace/spin balance\n" +
            "• Batter vs bowler matchups\n" +
            "• Venue history\n" +
            "• Home advantage\n" +
            "• Pitch characteristics\n" +
            "• Boundary dimensions\n" +
            "• Toss and toss decision\n" +
            "• Weather\n" +
            "• Temperature\n" +
            "• Humidity\n" +
            "• Wind\n" +
            "• Dew\n" +
            "• Rain/DLS conditions\n" +
            "• Head-to-head history\n" +
            "• Player recent form\n" +
            "• Player availability/injuries\n" +
            "• Rest and schedule congestion\n" +
            "• Umpire information where statistically useful\n" +
            "• Match stage\n" +
            "• Chasing vs defending performance\n" +
            "• Historical ball-by-ball patterns\n\n" +
            "Weak or unreliable factors will NOT be allowed to dominate the model."

        factors.textSize = 15f
        root.addView(factors)

        setContentView(scrollView)
    }

    private fun addTitle(
        root: LinearLayout,
        text: String
    ) {
        val title = TextView(this)
        title.text = text
        title.textSize = 28f
        title.setTextColor(Color.BLACK)
        title.gravity = Gravity.CENTER
        title.setPadding(0, 0, 0, 8)

        root.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun addSubtitle(
        root: LinearLayout,
        text: String
    ) {
        val subtitle = TextView(this)
        subtitle.text = text
        subtitle.textSize = 16f
        subtitle.gravity = Gravity.CENTER
        subtitle.setPadding(0, 0, 0, 24)

        root.addView(subtitle)
    }

    private fun addSection(
        root: LinearLayout,
        text: String
    ) {
        val section = TextView(this)
        section.text = text
        section.textSize = 20f
        section.setTextColor(Color.rgb(30, 80, 160))
        section.setPadding(0, 28, 0, 12)

        root.addView(section)
    }

    private fun addLabel(
        root: LinearLayout,
        text: String
    ) {
        val label = TextView(this)
        label.text = text
        label.textSize = 15f
        label.setPadding(0, 12, 0, 4)

        root.addView(label)
    }

    private fun addSpinner(
        root: LinearLayout,
        labelText: String,
        values: Array<String>
    ): Spinner {

        addLabel(root, labelText)

        val spinner = Spinner(this)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            values
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner.adapter = adapter

        root.addView(
            spinner,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        return spinner
    }

    private fun showTemporaryResult(
        team1: String,
        team2: String
    ) {
        if (team1 == team2) {
            resultText.text =
                "Please select two different teams."
            return
        }

        resultText.text =
            "MATCH CONFIGURATION SAVED\n\n" +
            "$team1 vs $team2\n\n" +
            "The probability engine is not using fake percentages.\n\n" +
            "NEXT STAGE:\n" +
            "Connect real historical cricket data and the player database, then calculate probabilities from the statistical model."
    }
}
