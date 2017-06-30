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
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley


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
    var gameState = GameState.EnterWords
    var autoCompleteAdapter: ArrayAdapter<String>? = null
    var boardLayout: TableLayout? = null
    var requestQueue : RequestQueue? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        boardLayout = findViewById(R.id.board) as TableLayout
        autoCompleteAdapter = ArrayAdapter<String>(this,
                R.layout.autocomplete_list_item,
                getResources().getStringArray(R.array.wordlist))

        val remainingLayout = findViewById(R.id.remaining_layout) as ViewGroup

        val defaultWords = Array<Array<Word>>(5) {
            Array<Word>(5){
                Word("", WordType.Civilian, false)
            }
        }
        board = Board(defaultWords, WordType.Red, boardLayout!!, remainingLayout, autoCompleteAdapter!!, gameState, this)

        val clueListView = findViewById(R.id.clue_list) as RecyclerView
        clueListView.setLayoutManager(LinearLayoutManager(this));
        clueList = ClueList(clueListView, this)
        val clueListAdapter = ClueListAdapter(clueList!!, {})
        clueListView.setAdapter(clueListAdapter);

        requestQueue = Volley.newRequestQueue(this)

        nextGameState.setOnClickListener { _ ->
            if(gameState == GameState.EnterWords){
                gameState = GameState.EnterColors
            }
            else if(gameState == GameState.EnterColors){
                gameState = GameState.GetClues
            }
            updateLayout()
        }

        previousGameState.setOnClickListener { _ ->
            if(gameState == GameState.EnterColors){
                gameState = GameState.EnterWords
            }
            else if(gameState == GameState.GetClues){
                gameState = GameState.EnterColors
            }
            updateLayout()
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
            val bot = Bot()
            bot.init(board!!)
            bot.getClue(Team.Red, requestQueue!!, { clue: Clue ->
                val dialog = ClueDialog()
                clueList!!.addClue(clue)
                dialog.clue = clue
                dialog.show(getFragmentManager(), "clue")
            })
        }

        get_blue_clue_button.setOnClickListener { _ ->
            val bot = Bot()
            bot.init(board!!)
            bot.getClue(Team.Blue, requestQueue!!, { clue: Clue ->
                val dialog = ClueDialog()
                clueList!!.addClue(clue)
                dialog.clue = clue
                dialog.show(getFragmentManager(), "clue")
            })
        }

        take_a_photo.setOnClickListener { _ ->
            launchCamera()
        }

        updateLayout()
    }
    
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    val i = Intent(this, CameraActivity::class.java)
                    val requestCode = if(gameState == GameState.EnterWords) RequestCode.WordRecognition else RequestCode.GridRecognition
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
                1);
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun updateNavigationButtons(){
        val forwardColor = if(gameState == GameState.GetClues) R.color.navigation_disabled else R.color.navigation_enabled
        nextGameState.setBackgroundTintList(getResources().getColorStateList(
                forwardColor,
                getTheme()))
        nextGameState.invalidate()

        val backColor = if(gameState == GameState.EnterWords) R.color.navigation_disabled else R.color.navigation_enabled
        previousGameState.setBackgroundTintList(getResources().getColorStateList(
                backColor,
                getTheme()))
        nextGameState.invalidate()
    }

    fun updateBoard(){
        val remainingLayout = findViewById(R.id.remaining_layout) as ViewGroup
        board = Board(board!!.words, board!!.paintType, boardLayout!!, remainingLayout, autoCompleteAdapter!!, gameState, this)
    }

    fun updateLayout(){
        updateNavigationButtons()
        updateBoard()

        val colorPickerVisibility = if(gameState == GameState.EnterColors) VISIBLE else INVISIBLE
        place_red_spy_button.visibility = colorPickerVisibility
        place_blue_spy_button.visibility = colorPickerVisibility
        place_civilian_button.visibility = colorPickerVisibility
        place_assassin_button.visibility = colorPickerVisibility

        when(gameState){
            GameState.EnterWords -> {
                instructions.setText(R.string.instructions_enter_words)
                take_a_photo_layout.visibility = VISIBLE
                clue_layout.visibility = INVISIBLE
                remaining_layout.visibility = INVISIBLE
                clue_list.visibility = INVISIBLE
            }

            GameState.EnterColors -> {
                instructions.setText(R.string.instructions_enter_colors)
                take_a_photo_layout.visibility = VISIBLE
                clue_layout.visibility = INVISIBLE
                remaining_layout.visibility = INVISIBLE
                clue_list.visibility = INVISIBLE
            }

            GameState.GetClues -> {
                instructions.setText(R.string.instructions_get_clues)
                take_a_photo_layout.visibility = INVISIBLE
                clue_layout.visibility = VISIBLE
                remaining_layout.visibility = VISIBLE
                clue_list.visibility = VISIBLE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            R.id.action_new_game -> {
                val builder = AlertDialog.Builder(this)

                builder.setMessage(R.string.new_game_message)
                        .setTitle(R.string.new_game_title)
                builder.setPositiveButton(R.string.new_game_yes, DialogInterface.OnClickListener { dialog, id ->
                    val remainingLayout = findViewById(R.id.remaining_layout) as ViewGroup
                    val defaultWords = Array<Array<Word>>(5) {
                        Array<Word>(5){
                            Word("", WordType.Civilian, false)
                        }
                    }

                    board = Board(defaultWords, WordType.Red, boardLayout!!, remainingLayout, autoCompleteAdapter!!, gameState, this)
                    gameState = GameState.EnterWords
                    updateLayout()
                    clueList!!.clear()
                })
                builder.setNegativeButton(R.string.new_game_no, DialogInterface.OnClickListener { dialog, id ->
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
    override fun onSaveInstanceState(outState: Bundle)
    {
        outState.putString("game_state", gameState.toString());
        board?.onSaveInstanceState(outState, "board")
        super.onSaveInstanceState(outState);
    }

    override fun onRestoreInstanceState(inState: Bundle)
    {
        val newGameState = GameState.valueOf(inState.getString("game_state"))
        board!!.onRestoreInstanceState(inState, "board")
        if(newGameState != gameState){
            gameState = GameState.valueOf(inState.getString("game_state"))
            updateLayout()
        }
        board!!.updateLayout()
        super.onRestoreInstanceState(inState)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when(requestCode) {
            RequestCode.WordRecognition.ordinal -> if (resultCode == Activity.RESULT_OK) {
                for(i in 0 until 5){
                    for(j in 0 until 5){
                        val key = "word" + i.toString() + "_" + j.toString()
                        val word = data.extras[key] as String
                        board!!.words[i][j].word = word
                    }
                }
                board!!.updateLayout()
            }

            RequestCode.GridRecognition.ordinal -> if (resultCode == Activity.RESULT_OK) {
                for(i in 0 until 5){
                    for(j in 0 until 5){
                        val key = "word" + i.toString() + "_" + j.toString()
                        val character = data.extras[key] as String
                        val wordType : WordType? =
                            when(character){
                                "a" -> WordType.Assassin
                                "c" -> WordType.Civilian
                                "b" -> WordType.Blue
                                "r" -> WordType.Red
                                else -> {
                                    null
                                }
                            }
                        if(wordType != null) {
                            board!!.words[i][j].type = wordType
                        }
                    }
                }
                board!!.updateLayout()
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
