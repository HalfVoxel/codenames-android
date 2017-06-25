package com.gullesnuffs.codenames

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.*
import android.view.LayoutInflater
import android.view.View


class Board(var words: Array<Array<Word>>,
            var paintType: WordType,
            var layout: TableLayout,
            val autoCompleteAdapter: ArrayAdapter<String>,
            val gameState: GameState,
            val context: Context){

    var width: Int = words[0].size
    var height: Int = words.size
    var wordLayouts: Array<Array<LinearLayout>>? = null
    var textViews: Array<Array<TextView>>? = null

    init{

        val inflater = LayoutInflater.from(context)

        layout.removeAllViews()

        wordLayouts = Array<Array<LinearLayout>>(height)
        {
            i ->
            val row : TableRow = TableRow(context)
            layout.addView(row)
            Array<LinearLayout>(width) {
                j ->
                if(gameState == GameState.EnterWords) {
                    inflater.inflate(R.layout.editable_word, row)
                }
                else{
                    inflater.inflate(R.layout.word, row)
                }
                row.getChildAt(j) as LinearLayout
            }
        }

        textViews = Array<Array<TextView>>(height)
        {
            i -> Array<TextView>(width)
            {
                j ->
                val wordLayout = wordLayouts!![i][j]
                val firstChild = wordLayout.getChildAt(0)
                when(gameState) {
                    GameState.EnterWords -> {
                        val textView: AutoCompleteTextView = firstChild as AutoCompleteTextView
                        textView.setAdapter<ArrayAdapter<String>>(autoCompleteAdapter)

                        val editFilters = textView.getFilters()
                        val newFilters = Array<InputFilter>(editFilters.size + 1) {
                            i ->
                            if(i < editFilters.size){
                                editFilters[i]
                            }
                            else{
                                InputFilter.AllCaps()
                            }
                        }
                        textView.setFilters(newFilters)

                        textView.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?,
                                                           start: Int, count: Int, after: Int) {
                            }

                            override fun afterTextChanged(s: Editable?) {
                            }

                            override fun onTextChanged(s: CharSequence?,
                                                       start: Int, before: Int, count: Int) {
                                words[i][j].word = s.toString()
                            }
                        })
                        textView
                    }
                    GameState.EnterColors -> {
                        wordLayout.setOnClickListener(object: View.OnClickListener {
                            override fun onClick(view: View): Unit {
                                words[i][j].type = paintType
                                updateLayout()
                            }
                        })
                        val textView: TextView = firstChild as TextView
                        textView
                    }
                    GameState.GetClues -> {
                        wordLayout.setOnClickListener(object: View.OnClickListener {
                            override fun onClick(view: View): Unit {
                                words[i][j].contacted = !words[i][j].contacted
                                updateLayout()
                            }
                        })
                        val textView: TextView = firstChild as TextView
                        textView
                    }
                }
            }
        }

        updateLayout()
    }

    fun updateLayout(){
        for(r in 0..(height-1)) {
            for (c in 0..(width - 1)) {
                textViews!![r][c].apply {
                    text = words[r][c].word
                    setBackgroundColor(words[r][c].getColor(gameState))
                    invalidate()
                }
            }
        }
    }

    fun onSaveInstanceState(outState: Bundle, prefix: String){
        outState.putInt(prefix + "_width", width)
        outState.putInt(prefix + "_height", height)
        outState.putString(prefix + "_paint_type", paintType.toString())
        for(i in 0 until width) {
            for (j in 0 until height) {
                words[i][j].onSaveInstanceState(outState, prefix + "_word_" + i + "_" + j);
            }
        }
    }

    fun onRestoreInstanceState(inState: Bundle, prefix: String){
        width = inState.getInt(prefix + "_width")
        height = inState.getInt(prefix + "_height")
        paintType = WordType.valueOf(inState.getString(prefix + "_paint_type"))
        words = Array<Array<Word>>(height) {
            i -> Array<Word>(width) {
                j -> Word(inState, prefix + "_word_" + i + "_" + j)
            }
        }
    }
}