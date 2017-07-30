package com.gullesnuffs.codenames

import android.Manifest
import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TableLayout
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import android.content.DialogInterface
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.constraint.ConstraintSet
import android.support.transition.AutoTransition
import android.support.transition.TransitionManager
import android.util.Log
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.gullesnuffs.codenames.R.string.pref_inappropriate_default
import com.gullesnuffs.codenames.R.string.pref_optimism_default
import kotlinx.android.synthetic.main.clue_dialog.view.*
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

    val constraintSet1 = ConstraintSet()
    val constraintSetPlay = ConstraintSet()
    val constraintSetWords = ConstraintSet()
    val constraintSetColors = ConstraintSet()

    var clueListView: RecyclerView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        constraintSet1.clone(constraintLayout)
        constraintSetPlay.clone(this, R.layout.content_main_play)
        constraintSetWords.clone(this, R.layout.content_main_words)
        constraintSetColors.clone(this, R.layout.content_main_colors)

        boardLayout = findViewById(R.id.board) as TableLayout
        autoCompleteAdapter = ArrayAdapter<String>(this,
                R.layout.autocomplete_list_item,
                resources.getStringArray(R.array.wordlist))

        val remainingLayout = findViewById(R.id.remaining_layout) as ViewGroup

        val board = Board(boardLayout!!, remainingLayout, autoCompleteAdapter!!, gameState, this)
        this.board = board

        val clueListView = findViewById(R.id.clue_list) as RecyclerView
        this.clueListView = clueListView
        clueListView.layoutManager = LinearLayoutManager(this);
        clueList = ClueList(clueListView, this)
        val clueListAdapter = ClueListAdapter(clueList!!, { clue ->
            val targetWords = clue.getTargetWords()
            if (clue == clueList!!.selectedClue.value) {
                board.displayScores = false
                clueList!!.selectedClue.value = null
            } else {
                board.displayScores = true

                for (i in 0 until board.height) {
                    for (j in 0 until board.width) {
                        val word = board.words[i][j]
                        word.isTarget = word.word.value.toLowerCase() in targetWords
                        word.score = clue.getWordScore(word.word.value.toLowerCase())
                    }
                }
                clueList!!.selectedClue.value = clue
            }
            board.animateCardScores()
        })
        clueListView.adapter = clueListAdapter;

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

        nextGameState.setOnClickListener { _ ->
            if (gameState.value == GameState.EnterWords) {
                gameState.value = GameState.EnterColors
            } else if (gameState.value == GameState.EnterColors) {
                gameState.value = GameState.GetClues
            }
        }

        place_red_spy_button.setOnClickListener { _ ->
            board!!.paintType = WordType.Red
        }

        place_blue_spy_button.setOnClickListener { _ ->
            board!!.paintType = WordType.Blue
        }

        place_civilian_button.setOnClickListener { _ ->
            board!!.paintType = WordType.Civilian
        }

        place_assassin_button.setOnClickListener { _ ->
            board!!.paintType = WordType.Assassin
        }

        get_red_clue_button.setOnClickListener { _ ->
            getClue(Team.Red)
        }

        get_blue_clue_button.setOnClickListener { _ ->
            getClue(Team.Blue)
        }

        take_a_photo.setOnClickListener { _ ->
            launchCamera()
        }

        randomize.setOnClickListener { _ -> randomize() }

        react({ onGameStateChanged() }, gameState)

        init(gameState)
    }

    fun getClue(team: Team) {
        var prefs = PreferenceManager.getDefaultSharedPreferences(this);
        val dialog = ClueDialog()
        dialog.team = team
        dialog.show(getFragmentManager(), "clue")
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

    fun randomize() {
        instructions.requestFocus()
        val anim = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        anim.duration = 500
        randomize.startAnimation(anim)

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
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    val i = Intent(this, CameraActivity::class.java)
                    val requestCode = if (gameState.value == GameState.EnterWords) RequestCode.WordRecognition else RequestCode.GridRecognition
                    i.putExtra("RequestCode", requestCode)
                    startActivityForResult(i, requestCode.ordinal)
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

    fun onGameStateChanged() {
        val transition = AutoTransition()
        transition.duration = 150
        TransitionManager.beginDelayedTransition(constraintLayout, transition)
        val constraint = when (gameState.value) {
            GameState.EnterWords -> constraintSetWords
            GameState.EnterColors -> constraintSetColors
            GameState.GetClues -> constraintSetPlay
        }
        constraint.applyTo(constraintLayout)

        when (gameState.value) {
            GameState.EnterWords -> {
                instructions.setText(R.string.instructions_enter_words)
                optionsMenu?.findItem(R.id.action_new_game)?.setVisible(false)
            }

            GameState.EnterColors -> {
                instructions.setText(R.string.instructions_enter_colors)
                optionsMenu?.findItem(R.id.action_new_game)?.setVisible(false)
            }

            GameState.GetClues -> {
                instructions.setText(R.string.instructions_get_clues)
                optionsMenu?.findItem(R.id.action_new_game)?.setVisible(true)
            }
        }

        var cursorVisible = (gameState.value == GameState.EnterWords)

        for(card in board!!.cards){
            card.setCursorVisible(cursorVisible)
            card.setFocusable(cursorVisible)
            card.setFocusableInTouchMode(cursorVisible)
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
                builder.setNegativeButton(R.string.new_game_no, { dialog, id ->
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
        outState.putString("game_state", gameState.value.toString());
        board?.onSaveInstanceState(outState, "board")
        super.onSaveInstanceState(outState);
    }

    override fun onRestoreInstanceState(inState: Bundle) {
        gameState.value = GameState.valueOf(inState.getString("game_state"))
        board!!.onRestoreInstanceState(inState, "board")
        super.onRestoreInstanceState(inState)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCode.WordRecognition.ordinal -> {
                instructions.requestFocus()
                if (resultCode == Activity.RESULT_OK && data != null) {
                    for (i in 0 until 5) {
                        for (j in 0 until 5) {
                            val key = "word" + i.toString() + "_" + j.toString()
                            val word = data.extras[key] as String
                            board!!.words[i][j].word.value = word
                        }
                    }
                    nextGameState.requestFocus()
                }
            }
            RequestCode.GridRecognition.ordinal -> {
                instructions.requestFocus()
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

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
