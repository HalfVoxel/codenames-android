package com.gullesnuffs.codenames

import android.os.Bundle

class Explanation {
    val words = mutableListOf<String>()
    val scores = mutableListOf<Double>()
    val types = mutableListOf<WordType>()

    fun add(word: String, score: Double, type: String){
        words.add(word)
        scores.add(score)
        types.add(
            when(type){
                "r" -> WordType.Red
                "b" -> WordType.Blue
                "a" -> WordType.Assassin
                else -> WordType.Civilian
            }
        )
    }

    fun onSaveInstanceState(outState: Bundle, prefix: String){
        outState.putInt(prefix + "_count", words.size)
        for(i in 0 until words.size){
            outState.putString(prefix + "_word" + i, words[i])
            outState.putDouble(prefix + "_score" + i, scores[i])
            outState.putString(prefix + "_type" + i, types[i].toString())
        }
    }

    fun onRestoreInstanceState(inState: Bundle, prefix: String){
        val count = inState.getInt(prefix + "_count")
        for(i in 0 until count){
            words.add(inState.getString(prefix + "_word" + i))
            scores.add(inState.getDouble(prefix + "_score" + i))
            types.add(WordType.valueOf(inState.getString(prefix + "_type" + i)))
        }
    }
}