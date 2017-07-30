package com.gullesnuffs.codenames

import android.os.Bundle

enum class Team {
    Red,
    Blue
}

enum class WordType {
    Red,
    Blue,
    Civilian,
    Assassin
}

class Word(val row : Int, val column : Int) {
    var word = Observable("")
    var type = Observable(WordType.Civilian)

    /** True if the word has been taken */
    var contacted = Observable(false)

    /** True if word is part of the bot's target words for the active clue (i.e the clue that is being explained) */
    var isTarget = false

    /** True if word was not taken when the currently active clue was given.
     * This determines if the word will be visible on the board when the scores are visualized.
     */
    var isVisible = true

    /** Bot's score for the word for the currently active clue */
    var score = 0f

    override fun toString(): String {
        return word.value
    }

    fun getColor(gameState: GameState): Int {
        if (contacted.value || gameState == GameState.EnterColors) {
            return when (type.value) {
                WordType.Red -> R.color.red_team_color
                WordType.Blue -> R.color.blue_team_color
                WordType.Civilian -> R.color.civilian_color
                WordType.Assassin -> R.color.assassin_color
            }
        } else {
            return R.color.not_contacted_color
        }
    }

    fun getColorCode(): String {
        return when (type.value) {
            WordType.Red -> "r"
            WordType.Blue -> "b"
            WordType.Civilian -> "c"
            WordType.Assassin -> "a"
        }
    }

    fun onSaveInstanceState(outState: Bundle, prefix: String) {
        outState.putString(prefix + "_word", word.value)
        outState.putString(prefix + "_type", type.value.toString())
        outState.putBoolean(prefix + "_contacted", contacted.value)
    }

    fun onRestoreInstanceState(inState: Bundle, prefix: String) {
        word.value = inState.getString(prefix + "_word")
        type.value = WordType.valueOf(inState.getString(prefix + "_type"))
        contacted.value = inState.getBoolean(prefix + "_contacted")
    }
}