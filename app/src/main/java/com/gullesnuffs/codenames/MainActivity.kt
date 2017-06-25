package com.gullesnuffs.codenames

import android.annotation.TargetApi
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import kotlinx.android.synthetic.main.activity_main.*
import android.widget.TableLayout
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.content_main.*
import android.content.Intent
import android.support.v4.app.ActivityCompat
import android.widget.Toast
import android.content.pm.PackageManager



enum class GameState {
    EnterWords,
    EnterColors,
    GetClues
}

class MainActivity : AppCompatActivity() {

    var board: Board? = null
    var gameState = GameState.EnterWords
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
        board = Board(words, WordType.Red, boardLayout!!, autoCompleteAdapter!!, gameState, this)

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

        updateLayout()
        
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                1);
    }
    
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    //val i = Intent(this, CameraActivity::class.java)
                    //startActivity(i)
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
        board = Board(board!!.words, board!!.paintType, boardLayout!!, autoCompleteAdapter!!, gameState, this)
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
            GameState.EnterWords ->
                instructions.setText(R.string.instructions_enter_words)

            GameState.EnterColors ->
                instructions.setText(R.string.instructions_enter_colors)

            GameState.GetClues ->
                instructions.setText(R.string.instructions_get_clues)
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
            updateLayout()
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
