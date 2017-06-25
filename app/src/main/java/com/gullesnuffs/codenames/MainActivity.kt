package com.gullesnuffs.codenames

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import android.widget.TableLayout
import android.widget.ArrayAdapter


class MainActivity : AppCompatActivity() {

    var board: Board? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        val boardLayout: TableLayout = findViewById(R.id.board) as TableLayout
        val words: Array<Array<Word>> = Array<Array<Word>>(5) {
            Array<Word>(5){
                Word("", WordType.Civilian, false)
            }
        }
        val autoCompleteAdapter = ArrayAdapter<String>(this,
                R.layout.autocomplete_list_item,
                getResources().getStringArray(R.array.wordlist))
        board = Board(words, boardLayout, autoCompleteAdapter)
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
        board?.onSaveInstanceState(outState, "board")
        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }

    override fun onRestoreInstanceState(inState: Bundle)
    {
        board!!.onRestoreInstanceState(inState, "board")
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
