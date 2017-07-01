package com.gullesnuffs.codenames

import android.graphics.Color
import android.os.Bundle

enum class Team{
    Red,
    Blue
}

enum class WordType{
    Red,
    Blue,
    Civilian,
    Assassin
}

class Word(var word: String, var type: WordType, var contacted: Boolean){

    var isTarget = false

    constructor(inState: Bundle, prefix: String): this(
            inState.getString(prefix + "_word"),
            WordType.valueOf(inState.getString(prefix + "_type")),
            inState.getBoolean(prefix + "_contacted")){
    }

    override fun toString(): String{
        return word
    }

    fun getColor(gameState : GameState): Int{
        if(contacted || gameState == GameState.EnterColors) {
            return when (type) {
                WordType.Red -> R.color.red_team_color
                WordType.Blue -> R.color.blue_team_color
                WordType.Civilian -> R.color.civilian_color
                WordType.Assassin -> R.color.assassin_color
            }
        }
        else{
            return R.color.not_contacted_color;
        }
    }

    fun getColorCode(): String{
        return when (type) {
            WordType.Red -> "r"
            WordType.Blue -> "b"
            WordType.Civilian -> "c"
            WordType.Assassin -> "a"
        }
    }

    fun onSaveInstanceState(outState: Bundle, prefix: String){
        outState.putString(prefix + "_word", word)
        outState.putString(prefix + "_type", type.toString())
        outState.putBoolean(prefix + "_contacted", contacted)
    }
}