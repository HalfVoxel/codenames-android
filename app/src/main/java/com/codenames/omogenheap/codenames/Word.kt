package com.codenames.omogenheap.codenames

import android.graphics.Color

enum class WordType{
    Red,
    Blue,
    Civilian,
    Assassin
}

class Word(var word: String, var type: WordType, var contacted: Boolean){

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
}