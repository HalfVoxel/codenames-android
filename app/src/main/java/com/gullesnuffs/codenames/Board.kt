package com.gullesnuffs.codenames

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


class Board(var words: Array<Array<Word>>,
            var paintType: WordType,
            var layout: TableLayout,
            val remainingLayout: ViewGroup,
            val autoCompleteAdapter: ArrayAdapter<String>,
            val gameState: GameState,
            val context: Context) {
    var width: Int = words[0].size
    var height: Int = words.size
    val cards = ArrayList<Card>()
    val redSpiesRemainingView = remainingLayout.findViewById(R.id.red_spies_remaining) as TextView
    val blueSpiesRemainingView = remainingLayout.findViewById(R.id.blue_spies_remaining) as TextView
    val civiliansRemainingView = remainingLayout.findViewById(R.id.civilians_remaining) as TextView
    //val assassinsRemainingView = remainingLayout.findViewById(R.id.assassins_remaining) as TextView

    init {
        val inflater = LayoutInflater.from(context)

        layout.removeAllViews()

        for (i in 0 until height) {
            val row: TableRow = TableRow(context)
            layout.addView(row)
            for (j in 0 until width) {
                inflater.inflate(R.layout.editable_word, row)
                val card = (row.getChildAt(j) as LinearLayout).getChildAt(0) as Card
                cards.add(card)
            }
        }

        cards.zip(words.flatten()).forEach { (card, word) ->
            val parent = card.parent as ViewGroup
            when (gameState) {
                GameState.EnterWords -> {
                    card.setAdapter<ArrayAdapter<String>>(autoCompleteAdapter)
                    card.onChanged = { s -> word.word = s }
                    card.editable = true
                }
                GameState.EnterColors -> {
                    parent.setOnClickListener { _ ->
                        word.type = paintType
                        updateLayout()
                    }
                    card.editable = false
                }
                GameState.GetClues -> {
                    parent.setOnClickListener { _ ->
                        word.contacted = !word.contacted
                        updateLayout()
                    }
                    card.editable = false
                }
            }
        }

        initializeFocus(cards)
        updateLayout()
    }

    fun initializeFocus(textViews: ArrayList<Card>) {
        for (textView in textViews) {
            textView.id = View.generateViewId()
        }

        textViews.forEachIndexed { index, textView ->
            if (index + 1 < textViews.size) {
                textView.nextFocusDownId = textViews[index + 1].id
            }
        }
    }

    fun updateLayout() {
        val remainingCount = intArrayOf(0, 0, 0, 0)
        cards.zip(words.flatten()).forEach { (card, word) ->
            if (!word.contacted) {
                remainingCount[word.type.ordinal]++
            }

            (card as TextView).text = word.word
            if (gameState == GameState.GetClues) {
                card.state = if (word.contacted) word.type else null
            } else {
                card.state = word.type
            }
        }

        layout.invalidate()

        redSpiesRemainingView.text = remainingCount[WordType.Red.ordinal].toString()
        blueSpiesRemainingView.text = remainingCount[WordType.Blue.ordinal].toString()
        civiliansRemainingView.text = remainingCount[WordType.Civilian.ordinal].toString()
        //assassinsRemainingView.text = remainingCount[WordType.Assassin.ordinal].toString()
        remainingLayout.invalidate()
    }

    fun onSaveInstanceState(outState: Bundle, prefix: String) {
        outState.putInt(prefix + "_width", width)
        outState.putInt(prefix + "_height", height)
        outState.putString(prefix + "_paint_type", paintType.toString())
        for (i in 0 until width) {
            for (j in 0 until height) {
                words[i][j].onSaveInstanceState(outState, prefix + "_word_" + i + "_" + j);
            }
        }
    }

    fun onRestoreInstanceState(inState: Bundle, prefix: String) {
        width = inState.getInt(prefix + "_width")
        height = inState.getInt(prefix + "_height")
        paintType = WordType.valueOf(inState.getString(prefix + "_paint_type"))
        words = Array<Array<Word>>(height) {
            i ->
            Array<Word>(width) {
                j ->
                Word(inState, prefix + "_word_" + i + "_" + j)
            }
        }
    }
}