package com.gullesnuffs.codenames

import android.os.Bundle

class Clue(var word: String, var number: Int, var team: Team) {

    var explanation: Explanation? = null

    override fun toString(): String {
        return word + "  " + number.toString()
    }

    fun getColor(): Int {
        return when (team) {
            Team.Red -> R.color.red_team_color
            Team.Blue -> R.color.blue_team_color
        }
    }

    fun getColorCode(): String {
        return when (team) {
            Team.Red -> "r"
            Team.Blue -> "b"
        }
    }

    fun onSaveInstanceState(outState: Bundle, prefix: String) {
        outState.putString(prefix + "_word", word)
        outState.putInt(prefix + "_number", number)
        outState.putString(prefix + "_team", team.toString())
        explanation?.onSaveInstanceState(outState, prefix + "_explanation")
    }

    fun onRestoreInstanceState(inState: Bundle, prefix: String) {
        word = inState.getString(prefix + "_word")
        number = inState.getInt(prefix + "_number")
        team = Team.valueOf(inState.getString(prefix + "_team"))
        explanation = Explanation()
        explanation!!.onRestoreInstanceState(inState, prefix + "_explanation")
    }

    fun getWordScore(word : String): Float {
        val exp = explanation ?: return 0f

        for (i in 0 until exp.words.size) {
            if (exp.words[i] == word) {
                return exp.scores[i].toFloat()
            }
        }

        return 0f
    }
    fun getTargetWords(): List<String> {
        val wordList = mutableListOf<String>()
        if (explanation == null)
            return wordList
        val desiredType = if (team == Team.Red) WordType.Red else WordType.Blue
        for (i in 0 until explanation!!.words.size) {
            if (explanation!!.types[i] == desiredType) {
                wordList.add(explanation!!.words[i])
                if (wordList.size == number) {
                    break
                }
            }
        }
        return wordList
    }
}