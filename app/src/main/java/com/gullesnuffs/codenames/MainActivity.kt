package com.gullesnuffs.codenames

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.view.animation.Transformation
import android.widget.ArrayAdapter
import android.widget.TableLayout
import android.widget.Toast
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.gullesnuffs.codenames.R.string.pref_inappropriate_default
import com.gullesnuffs.codenames.R.string.pref_optimism_default
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.content_main_colors.*
import kotlinx.android.synthetic.main.content_main_play.*
import kotlinx.android.synthetic.main.content_main_words.*
import java.util.*


enum class GameState {
    EnterWords,
    EnterColors,
    GetClues
}

enum class RequestCode {
    WordRecognition,
    GridRecognition
}

class MainActivity : AppCompatActivity() {

    var board: Board? = null
    var clueList: ClueList? = null
    val gameState = Observable(GameState.EnterWords)
    var autoCompleteAdapter: ArrayAdapter<String>? = null
    var boardLayout: TableLayout? = null
    var requestQueue: RequestQueue? = null
    var optionsMenu: Menu? = null

    var clueListView: RecyclerView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        boardLayout = findViewById(R.id.board) as TableLayout
        autoCompleteAdapter = ArrayAdapter<String>(this,
                R.layout.autocomplete_list_item,
                resources.getStringArray(R.array.wordlist))

        val remainingLayout = findViewById(R.id.remaining_layout) as ViewGroup

        val board = Board(boardLayout!!, remainingLayout, autoCompleteAdapter!!, gameState, this)
        this.board = board

        val clueListView = findViewById(R.id.clue_list) as RecyclerView
        this.clueListView = clueListView
        clueListView.layoutManager = LinearLayoutManager(this)
        clueList = ClueList(clueListView, this)
        val clueListAdapter = ClueListAdapter(clueList!!, { clue ->
            val targetWords = clue.getTargetWords()
            val visibleWords = clue.explanation?.words ?: mutableListOf<String>()
            if (clue == clueList!!.selectedClue.value) {
                board.displayScores = false
                clueList!!.selectedClue.value = null
            } else {
                board.displayScores = true

                for (i in 0 until board.height) {
                    for (j in 0 until board.width) {
                        val word = board.words[i][j]
                        word.isTarget = word.word.value.toLowerCase() in targetWords
                        word.isVisible = word.word.value.toLowerCase() in visibleWords
                        word.score = clue.getWordScore(word.word.value.toLowerCase())
                    }
                }
                clueList!!.selectedClue.value = clue
            }
            board.animateCardScores()
        })
        clueListView.adapter = clueListAdapter

        board.onClickCard = {
            word ->

            if (clueList!!.selectedClue.value != null) {
                board.displayScores = false
                clueList!!.selectedClue.value = null
                board.animateCardScores()
            } else if (gameState.value == GameState.EnterColors) {
                word.type.value = board.paintType
            } else if (gameState.value == GameState.GetClues) {
                word.contacted.value = !word.contacted.value
            }
        }

        requestQueue = Volley.newRequestQueue(this)

        place_red_spy_button.setOnClickListener { _ ->
            board.paintType = WordType.Red
        }

        place_blue_spy_button.setOnClickListener { _ ->
            board.paintType = WordType.Blue
        }

        place_civilian_button.setOnClickListener { _ ->
            board.paintType = WordType.Civilian
        }

        place_assassin_button.setOnClickListener { _ ->
            board.paintType = WordType.Assassin
        }

        get_red_clue_button.setOnClickListener { _ ->
            getClue(Team.Red)
        }

        get_blue_clue_button.setOnClickListener { _ ->
            getClue(Team.Blue)
        }

        state_colors.findViewById(R.id.nextGameState).setOnClickListener { _ -> nextGameState() }
        state_words.findViewById(R.id.nextGameState).setOnClickListener { _ -> nextGameState() }

        state_colors.findViewById(R.id.take_a_photo).setOnClickListener { _ -> launchCamera() }
        state_words.findViewById(R.id.take_a_photo).setOnClickListener { _ -> launchCamera() }

        state_colors.findViewById(R.id.randomize).setOnClickListener { _ -> randomize() }
        state_words.findViewById(R.id.randomize).setOnClickListener { _ -> randomize() }

        react({ onGameStateChanged() }, gameState)
    }

    override fun onResume() {
        super.onStart()
        init(gameState)
    }

    fun nextGameState() {
        if (gameState.value == GameState.EnterWords) {
            gameState.value = GameState.EnterColors
        } else if (gameState.value == GameState.EnterColors) {
            gameState.value = GameState.GetClues
        }
    }

    fun getClue(team: Team) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val dialog = ClueDialog()
        dialog.team = team
        dialog.show(fragmentManager, "clue")
        val bot = Bot(board!!)
        bot.getClue(team,
                requestQueue!!,
                clueList,
                prefs.getString("pref_optimism", resources.getString(pref_optimism_default)),
                prefs.getString("pref_inappropriate", resources.getString(pref_inappropriate_default)),
                { clue: Clue ->
                    addClue(dialog, clue)
                })
    }

    fun clearFocus() {
        // Set focus to some control that cannot accept it
        state_colors.findViewById(R.id.instructions).requestFocus()
    }

    fun randomize() {
        clearFocus()
        val anim = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        anim.duration = 500
        val rand1 = state_colors.findViewById(R.id.randomize)
        val rand2 = state_words.findViewById(R.id.randomize)
        rand1.startAnimation(anim)
        rand2.startAnimation(anim)

        when (gameState.value) {
            GameState.EnterWords -> {
                val words = resources.getStringArray(R.array.wordlist)
                val usedWords = mutableListOf<String>()

                board!!.flashCards({ _, word ->
                    var w: String
                    do {
                        w = words[(Math.random() * words.size).toInt()]
                    } while (w in usedWords)
                    usedWords.add(w)
                    word.word.value = w
                })
            }

            GameState.EnterColors -> {
                val colors = mutableListOf<WordType>()
                colors.add(WordType.Assassin)
                for (i in 0 until 7)
                    colors.add(WordType.Civilian)
                for (i in 0 until 8)
                    colors.add(WordType.Red)
                for (i in 0 until 8)
                    colors.add(WordType.Blue)
                if (Math.random() < 0.5)
                    colors.add(WordType.Red)
                else
                    colors.add(WordType.Blue)
                Collections.shuffle(colors)

                val flattened = board!!.words.flatten()
                board!!.flashCards({ _, word ->
                    word.type.value = colors[flattened.indexOf(word)]
                })
            }
            GameState.GetClues -> {
            }
        }
    }

    fun clearBoard() {
        board!!.words.flatten().forEach {
            it.word.value = ""
            it.type.value = WordType.Civilian
            it.contacted.value = false
            it.score = 0f
            it.isTarget = false
        }
        board!!.resetCardOverrideColors()
    }

    override fun onBackPressed() {
        if (gameState.value == GameState.EnterColors) {
            gameState.value = GameState.EnterWords
        } else if (gameState.value == GameState.GetClues) {
            gameState.value = GameState.EnterColors
        } else {
            super.onBackPressed()
        }

        board!!.displayScores = false
        clueList!!.selectedClue.value = null
        board!!.resetCardOverrideColors()
    }

    fun addClue(dialog: ClueDialog, clue: Clue) {
        clueList!!.addClue(clue)
        dialog.clue = clue
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    val i = Intent(this, CameraActivity::class.java)
                    val recognitionType = if (gameState.value == GameState.EnterWords) RequestCode.WordRecognition else RequestCode.GridRecognition
                    i.putExtra("RequestCode", recognitionType)
                    startActivityForResult(i, recognitionType.ordinal)
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this@MainActivity, "Permission denied!", Toast.LENGTH_SHORT).show()
                    System.exit(1)
                }
                return
            }
        }// other 'case' lines to check for other
        // permissions this app might request
    }

    fun launchCamera() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                1)
    }

    fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t;
    }

    fun onGameStateChanged() {
        val initialX = state_words.x
        // Note that the width of the view cannot be used because this may be called before
        // the first layout pass has been done
        val width = Resources.getSystem().displayMetrics.widthPixels.toFloat()
        val targetX = -gameState.value.ordinal * width
        val anim = object : Animation() {
            override fun applyTransformation(time: Float, t: Transformation?) {
                val x = lerp(initialX, targetX, time)
                state_words.x = x
                state_colors.x = x + width * 1
                state_play.x = x + width * 2

                state_words.invalidate()
                state_colors.invalidate()
                state_play.invalidate()
            }
        }

        anim.startOffset = 0
        anim.interpolator = FastOutSlowInInterpolator()
        anim.duration = 400
        state_group.startAnimation(anim)

        optionsMenu?.findItem(R.id.action_new_game)?.isVisible = when (gameState.value) {
            GameState.EnterWords -> false
            GameState.EnterColors -> false
            GameState.GetClues -> true
        }

        val cursorVisible = (gameState.value == GameState.EnterWords)

        for (card in board!!.cards) {
            card.isCursorVisible = cursorVisible
            card.isFocusable = cursorVisible
            card.isFocusableInTouchMode = cursorVisible
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        optionsMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val i = Intent(this, SettingsActivity::class.java)
                startActivity(i)
                true
            }
            R.id.action_new_game -> {
                val builder = AlertDialog.Builder(this)

                builder.setMessage(R.string.new_game_message)
                        .setTitle(R.string.new_game_title)
                builder.setPositiveButton(R.string.new_game_yes, { _, _ ->
                    clearBoard()
                    gameState.value = GameState.EnterWords
                    clueList!!.clear()
                })
                builder.setNegativeButton(R.string.new_game_no, { _, _ ->
                    // User cancelled the dialog
                })

                val dialog = builder.create()
                dialog.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // invoked when the activity may be temporarily destroyed, save the instance state here
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("game_state", gameState.value.toString())
        board?.onSaveInstanceState(outState, "board")
        clueList?.onSaveInstanceState(outState, "clueList")
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(inState: Bundle) {
        gameState.value = GameState.valueOf(inState.getString("game_state"))
        board!!.onRestoreInstanceState(inState, "board")
        clueList!!.onRestoreInstanceState(inState, "clueList")
        super.onRestoreInstanceState(inState)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCode.WordRecognition.ordinal -> {
                clearFocus()
                if (resultCode == Activity.RESULT_OK && data != null) {
                    for (i in 0 until 5) {
                        for (j in 0 until 5) {
                            val key = "word" + i.toString() + "_" + j.toString()
                            val word = data.extras[key] as String
                            board!!.words[i][j].word.value = word
                        }
                    }
                    clearFocus()
                }
            }
            RequestCode.GridRecognition.ordinal -> {
                clearFocus()
                if (resultCode == Activity.RESULT_OK && data != null) {
                    for (i in 0 until 5) {
                        for (j in 0 until 5) {
                            val key = "word" + i.toString() + "_" + j.toString()
                            val character = data.extras[key] as String
                            val wordType: WordType? =
                                    when (character) {
                                        "a" -> WordType.Assassin
                                        "c" -> WordType.Civilian
                                        "b" -> WordType.Blue
                                        "r" -> WordType.Red
                                        else -> {
                                            null
                                        }
                                    }
                            if (wordType != null) {
                                board!!.words[i][j].type.value = wordType
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
