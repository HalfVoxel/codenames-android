package com.gullesnuffs.codenames

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import android.widget.TableLayout
import android.widget.ArrayAdapter

enum class GameState {
    EnterWords,
    EnterColors,
    GetClues
}

class MainActivity : AppCompatActivity() {

    var board: Board? = null
    var gameState: GameState = GameState.EnterWords
    var autoCompleteAdapter: ArrayAdapter<String>? = null
    var boardLayout: TableLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        boardLayout = findViewById(R.id.board) as TableLayout
        val words: Array<Array<Word>> = Array<Array<Word>>(5) {
            Array<Word>(5){
                Word("", WordType.Civilian, false)
            }
        }
        autoCompleteAdapter = ArrayAdapter<String>(this,
                R.layout.autocomplete_list_item,
                getResources().getStringArray(R.array.wordlist))
        board = Board(words, boardLayout!!, autoCompleteAdapter!!, gameState, this)

        nextGameState.setOnClickListener { view ->
            if(gameState == GameState.EnterWords){
                gameState = GameState.EnterColors
            }
            else if(gameState == GameState.EnterColors){
                gameState = GameState.GetClues
            }
            board = Board(board!!.words, boardLayout!!, autoCompleteAdapter!!, gameState, this)
        }

        previousGameState.setOnClickListener { view ->
            if(gameState == GameState.EnterColors){
                gameState = GameState.EnterWords
            }
            else if(gameState == GameState.GetClues){
                gameState = GameState.EnterColors
            }
            board = Board(board!!.words, boardLayout!!, autoCompleteAdapter!!, gameState, this)
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
            board = Board(board!!.words, boardLayout!!, autoCompleteAdapter!!, gameState, this)
        }
        board!!.updateLayout()
        super.onRestoreInstanceState(inState)
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
