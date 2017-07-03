package com.gullesnuffs.codenames

import android.graphics.Color
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
    var contacted = Observable(false)
    var isTarget = false
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
            return R.color.not_contacted_color;
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