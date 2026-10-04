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

                    if (!entry.isDirectory &&
                        entry.name.lowercase(Locale.US).endsWith(".json")
                    ) {
                        val bytes = zip.readBytes()
                        val text = String(bytes, Charsets.UTF_8)
                        parseMatch(text, data)
                    }

                    zip.closeEntry()
                }
            }
        }

        historical = data
        data.matches > 0

    } catch (e: Exception) {
        false
    }
    }
