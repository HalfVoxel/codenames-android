package com.gullesnuffs.codenames

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*

class Board(var words: Array<Array<Word>>, var layout: TableLayout, val autoCompleteAdapter: ArrayAdapter<String>){

    var width: Int = words[0].size
    var height: Int = words.size
    var wordLayouts: Array<Array<LinearLayout>>? = null
    var textViews: Array<Array<TextView>>? = null

    init{

        wordLayouts = Array<Array<LinearLayout>>(height)
        {
            i ->
            val row = layout.getChildAt(i) as TableRow
            Array<LinearLayout>(width) {
                j -> row.getChildAt(j) as LinearLayout
            }
        }

        textViews = Array<Array<TextView>>(height)
        {
            i -> Array<TextView>(width)
            {
                j ->
                val textView : AutoCompleteTextView = wordLayouts!![i][j].getChildAt(0) as AutoCompleteTextView
                textView.setAdapter<ArrayAdapter<String>>(autoCompleteAdapter)
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
        }

        updateLayout()
    }

    fun updateLayout(){
        for(r in 0..(height-1)) {
            for (c in 0..(width - 1)) {
                textViews!![r][c].apply {
                    println("Found word in update layout: " + words[r][c])
                    text = words[r][c].word
                    setBackgroundColor(words[r][c].getColor())
                    invalidate()
                }
            }
        }
    }

    fun onSaveInstanceState(outState: Bundle, prefix: String){
        outState.putInt(prefix + "_width", width)
        outState.putInt(prefix + "_height", height)
        for(i in 0 until width) {
            for (j in 0 until height) {
                words[i][j].onSaveInstanceState(outState, prefix + "_word_" + i + "_" + j);
            }
        }
    }

    fun onRestoreInstanceState(inState: Bundle, prefix: String){
        width = inState.getInt(prefix + "_width")
        height = inState.getInt(prefix + "_height")
        words = Array<Array<Word>>(height) {
            i -> Array<Word>(width) {
                j -> Word(inState, prefix + "_word_" + i + "_" + j)
            }
        }
    }
}