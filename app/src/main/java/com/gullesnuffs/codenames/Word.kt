package com.gullesnuffs.codenames

import android.graphics.Color
import android.os.Bundle

enum class WordType{
    Red,
    Blue,
    Civilian,
    Assassin
}

class Word(var word: String, var type: WordType, var contacted: Boolean){

    constructor(inState: Bundle, prefix: String): this(
            inState.getString(prefix + "_word"),
            WordType.valueOf(inState.getString(prefix + "_type")),
            inState.getBoolean(prefix + "_contacted")){
    }

    override fun toString(): String{
        return word
    }

    fun getColor(): Int{
        if(contacted) {
            return when (type) {
                WordType.Red -> Color.RED
                WordType.Blue -> Color.BLUE
                WordType.Civilian -> Color.GRAY
                WordType.Assassin -> Color.BLACK
            }
        }
        else{
            return Color.WHITE;
        }
    }

    fun onSaveInstanceState(outState: Bundle, prefix: String){
        outState.putString(prefix + "_word", word)
        outState.putString(prefix + "_type", type.toString())
        outState.putBoolean(prefix + "_contacted", contacted)
    }
}