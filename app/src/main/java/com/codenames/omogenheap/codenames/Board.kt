package com.codenames.omogenheap.codenames

import android.content.Context
import android.view.LayoutInflater
import android.widget.*
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter



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
                textView
            }
        }

        updateLayout()
    }

    fun updateLayout(){
        for(r in 0..(height-1)) {
            for (c in 0..(width - 1)) {
                textViews!![r][c].apply {
                    setText(words[r][c].word)
                    setBackgroundColor(words[r][c].getColor())
                }
            }
        }
    }
}