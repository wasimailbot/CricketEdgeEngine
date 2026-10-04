package com.cricketedge.engine

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import java.util.zip.ZipInputStream

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
    private lateinit var statusText: TextView

    private val teams = arrayOf(
        "Royal Challengers Bengaluru", "Chennai Super Kings", "Mumbai Indians",
        "Kolkata Knight Riders", "Rajasthan Royals", "Sunrisers Hyderabad",
        "Delhi Capitals", "Punjab Kings", "Lucknow Super Giants", "Gujarat Titans"
    )

    private val venues = arrayOf(
        "Select Venue", "M. Chinnaswamy Stadium", "MA Chidambaram Stadium",
        "Wankhede Stadium", "Eden Gardens", "Narendra Modi Stadium",
        "Rajiv Gandhi International Stadium", "Arun Jaitley Stadium",
        "Sawai Mansingh Stadium", "Other / Unknown"
    )

    private val pitchTypes = arrayOf(
        "Select Pitch", "Batting Friendly", "Bowling Friendly", "Pace Friendly",
        "Spin Friendly", "Balanced", "Unknown"
    )

    private val dewOptions = arrayOf("Unknown", "Low", "Medium", "High")
    private val rainOptions = arrayOf("No Rain Expected", "Low Risk", "Medium Risk", "High Risk")
    private val tossDecisions = arrayOf("Not Known", "Bat First", "Field First")
    private val umpires = arrayOf("Not Selected", "Umpire 1", "Umpire 2", "Umpire 3", "Umpire 4")

    private data class TeamStats(var matches: Int = 0, var wins: Int = 0)
    private data class HistoricalData(
        val teams: MutableMap<String, TeamStats> = mutableMapOf(),
        val headToHead: MutableMap<String, Int> = mutableMapOf(),
        var matches: Int = 0
    )

    private var historical = HistoricalData()
    private var dataLoaded = false
    private var dataLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 48)
        }
        scrollView.addView(root)

        addTitle(root, "CRICKET EDGE ENGINE")
        addSubtitle(root, "Offline IPL historical-data win probability")

        statusText = TextView(this).apply {
            text = "Data status: checking bundled IPL database..."
            textSize = 14f
            setPadding(0, 0, 0, 18)
        }
        root.addView(statusText)

        addSection(root, "MATCH")
        team1Spinner = addSpinner(root, "Team 1", teams)
        team2Spinner = addSpinner(root, "Team 2", teams)
        homeTeamSpinner = addSpinner(root, "Home Team", arrayOf("Not Selected") + teams)

        addSection(root, "VENUE & PITCH")
        venueSpinner = addSpinner(root, "Venue", venues)
        pitchSpinner = addSpinner(root, "Pitch Characteristics", pitchTypes)

        addSection(root, "MATCH CONDITIONS")
        val temperature = addNumberField(root, "Temperature (°C)", "Example: 28")
        val humidity = addNumberField(root, "Humidity (%)", "Example: 65")
        val wind = addNumberField(root, "Wind Speed (km/h)", "Example: 12")
        dewSpinner = addSpinner(root, "Dew", dewOptions)
        rainSpinner = addSpinner(root, "Rain / Weather Risk", rainOptions)

        addSection(root, "TOSS")
        tossWinnerSpinner = addSpinner(root, "Toss Winner", arrayOf("Not Known") + teams)
        tossDecisionSpinner = addSpinner(root, "Toss Decision", tossDecisions)

        addSection(root, "UMPIRES")
        umpire1Spinner = addSpinner(root, "Umpire 1", umpires)
        umpire2Spinner = addSpinner(root, "Umpire 2", umpires)

        addSection(root, "PLAYING XI")
        val xiInfo = TextView(this).apply {
            text = "Playing XI database is not yet bundled. The probability engine will not invent player ratings."
            textSize = 15f
            setPadding(0, 8, 0, 12)
        }
        root.addView(xiInfo)

        addSection(root, "PROBABILITY ENGINE")
        calculateButton = Button(this).apply {
            text = "CALCULATE WIN PROBABILITY"
            textSize = 17f
            setOnClickListener {
                calculate(
                    team1Spinner.selectedItem.toString(),
                    team2Spinner.selectedItem.toString(),
                    homeTeamSpinner.selectedItem.toString(),
                    venueSpinner.selectedItem.toString(),
                    pitchSpinner.selectedItem.toString(),
                    tossWinnerSpinner.selectedItem.toString(),
                    tossDecisionSpinner.selectedItem.toString(),
                    dewSpinner.selectedItem.toString(),
                    rainSpinner.selectedItem.toString(),
                    temperature.text.toString(),
                    humidity.text.toString(),
                    wind.text.toString()
                )
            }
        }
        root.addView(calculateButton, matchParams())

        resultText = TextView(this).apply {
            text = "Select two teams and press CALCULATE."
            textSize = 17f
            setPadding(0, 24, 0, 20)
        }
        root.addView(resultText)

        addSection(root, "MODEL")
        val modelInfo = TextView(this).apply {
            text = "The score starts from real bundled IPL match history, then applies small transparent adjustments for home advantage, toss, pitch, dew and weather. No random percentage and no fake player statistics are used."
            textSize = 15f
        }
        root.addView(modelInfo)

        setContentView(scrollView)
        dataLoading = true
        calculateButton.isEnabled = false

        Thread {
            val loaded = loadHistoricalData()
            runOnUiThread {
                dataLoaded = loaded
                dataLoading = false
                statusText.text = if (loaded) {
                    "Data status: ${historical.matches} historical IPL matches loaded"
                } else {
                    "Data status: database could not be read; calculation is disabled."
                }
                calculateButton.isEnabled = loaded
            }
        }.start()
    }

    private fun loadHistoricalData(): Boolean {
        val data = HistoricalData()
        return try {
            assets.open("ipl_json.zip").use { input ->
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (!entry.isDirectory && entry.name.lowercase(Locale.US).endsWith(".json")) {
                            val text = BufferedReader(
                                InputStreamReader(zip, Charsets.UTF_8)
                            ).use { it.readText() }
                            parseMatch(text, data)
                        }
                        zip.closeEntry()
                    }
                }
            }
            historical = data
            data.matches > 0
        } catch (_: Exception) {
            false
        }
    }

    private fun parseMatch(text: String, data: HistoricalData) {
        try {
            val root = JSONObject(text)
            val info = root.optJSONObject("info") ?: root
            val teamsArray = info.optJSONArray("teams") ?: return
            if (teamsArray.length() < 2) return

            val a = teamsArray.optString(0)
            val b = teamsArray.optString(1)
            if (a.isBlank() || b.isBlank()) return

            val outcome = info.optJSONObject("outcome")
            val winner = outcome?.optString("winner", "") ?: ""
            if (winner.isBlank()) return

            data.matches++
            val sa = data.teams.getOrPut(a) { TeamStats() }
            val sb = data.teams.getOrPut(b) { TeamStats() }
            sa.matches++
            sb.matches++

            if (winner == a) sa.wins++
            else if (winner == b) sb.wins++
            else return

            val key = pairKey(a, b)
            data.headToHead[key] = (data.headToHead[key] ?: 0) +
                if (winner == a) 1 else -1
        } catch (_: Exception) {
            // Ignore malformed/non-match JSON files.
        }
    }

    private fun calculate(
        team1: String, team2: String, home: String, venue: String, pitch: String,
        tossWinner: String, tossDecision: String, dew: String, rain: String,
        temperatureText: String, humidityText: String, windText: String
    ) {
        if (team1 == team2) {
            resultText.text = "Please select two different teams."
            return
        }

        if (!dataLoaded || dataLoading) {
            resultText.text = "Historical IPL database is still loading. Please wait a moment."
            return
        }

        val s1 = historical.teams[team1]
        val s2 = historical.teams[team2]
        val p1 = historicalRate(s1)
        val p2 = historicalRate(s2)

        var score = logit(p1) - logit(p2)
        val reasons = mutableListOf<String>()

        if (s1 != null && s2 != null) {
            val h2h = historical.headToHead[pairKey(team1, team2)] ?: 0
            if (h2h != 0) {
                score += 0.08 * h2h.coerceIn(-8, 8)
                reasons += "historical head-to-head"
            }
        }

        if (home == team1) {
            score += 0.12
            reasons += "home advantage for $team1"
        }
        if (home == team2) {
            score -= 0.12
            reasons += "home advantage for $team2"
        }

        if (tossWinner == team1) {
            score += if (tossDecision == "Field First") 0.07 else 0.04
            reasons += "toss edge for $team1"
        } else if (tossWinner == team2) {
            score -= if (tossDecision == "Field First") 0.07 else 0.04
            reasons += "toss edge for $team2"
        }

        when (pitch) {
            "Batting Friendly" -> {
                score *= 0.97
                reasons += "batting-friendly pitch"
            }
            "Bowling Friendly", "Pace Friendly", "Spin Friendly" -> {
                score *= 1.02
                reasons += pitch.lowercase(Locale.US)
            }
        }

        when (dew) {
            "High" -> {
                score += if (tossDecision == "Field First") 0.04 else -0.02
                reasons += "high dew"
            }
            "Medium" -> score += if (tossDecision == "Field First") 0.02 else -0.01
        }

        when (rain) {
            "High Risk" -> {
                score *= 0.92
                reasons += "high rain/DLS uncertainty"
            }
            "Medium Risk" -> {
                score *= 0.97
                reasons += "medium rain uncertainty"
            }
        }

        val temperature = temperatureText.toDoubleOrNull()
        val humidity = humidityText.toDoubleOrNull()
        val wind = windText.toDoubleOrNull()

        if (temperature != null && temperature in 34.0..50.0) {
            score *= 0.98
            reasons += "high temperature"
        }
        if (humidity != null && humidity >= 80.0) {
            score *= 0.99
            reasons += "high humidity"
        }
        if (wind != null && wind >= 25.0) {
            score *= 0.98
            reasons += "strong wind"
        }

        val probability1 = sigmoid(score).coerceIn(0.05, 0.95)
        val probability2 = 1.0 - probability1

        resultText.text = buildString {
            append("${team1.uppercase()}  ${formatPct(probability1)}\n")
            append("${team2.uppercase()}  ${formatPct(probability2)}\n\n")
            append("Historical sample: ${s1?.matches ?: 0} matches for $team1; ")
            append("${s2?.matches ?: 0} for $team2.\n")
            append("Model input: bundled IPL historical JSON.\n\n")
            append("This is a statistical estimate, not a guarantee or betting advice.\n")
            if (reasons.isNotEmpty()) {
                append("\nApplied context: ${reasons.distinct().joinToString(", ")}.")
            }
        }
    }

    private fun historicalRate(stats: TeamStats?): Double {
        if (stats == null || stats.matches == 0) return 0.5
        return ((stats.wins.toDouble() + 2.0) / (stats.matches.toDouble() + 4.0))
            .coerceIn(0.05, 0.95)
    }

    private fun pairKey(a: String, b: String): String =
        if (a < b) "$a|||$b" else "$b|||$a"

    private fun logit(p: Double): Double = kotlin.math.ln(p / (1.0 - p))
    private fun sigmoid(x: Double): Double = 1.0 / (1.0 + kotlin.math.exp(-x))

    private fun formatPct(value: Double): String =
        String.format(Locale.US, "%.1f%%", value * 100.0)

    private fun addNumberField(
        root: LinearLayout,
        label: String,
        hint: String
    ): EditText {
        addLabel(root, label)
        val field = EditText(this).apply {
            this.hint = hint
            inputType = 2
        }
        root.addView(field, matchParams())
        return field
    }

    private fun addTitle(root: LinearLayout, text: String) {
        val title = TextView(this).apply {
            this.text = text
            textSize = 28f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }
        root.addView(title, matchParams())
    }

    private fun addSubtitle(root: LinearLayout, text: String) {
        val subtitle = TextView(this).apply {
            this.text = text
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        root.addView(subtitle)
    }

    private fun addSection(root: LinearLayout, text: String) {
        val section = TextView(this).apply {
            this.text = text
            textSize = 20f
            setTextColor(Color.rgb(30, 80, 160))
            setPadding(0, 28, 0, 12)
        }
        root.addView(section)
    }

    private fun addLabel(root: LinearLayout, text: String) {
        val label = TextView(this).apply {
            this.text = text
            textSize = 15f
            setPadding(0, 12, 0, 4)
        }
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
        root.addView(spinner, matchParams())
        return spinner
    }

    private fun matchParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
}
