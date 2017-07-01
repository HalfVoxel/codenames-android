package com.gullesnuffs.codenames

import android.os.Bundle

class Clue (var word: String, var number: Int, var team: Team){
    override fun toString(): String{
        return word + "  " + number.toString()
    }

    fun getColor(): Int{
        return when (team) {
            Team.Red -> R.color.red_team_color
            Team.Blue -> R.color.blue_team_color
        }
    }

    fun getColorCode(): String{
        return when (team) {
            Team.Red -> "r"
            Team.Blue -> "b"
        }
    }

    fun onSaveInstanceState(outState: Bundle, prefix: String){
        outState.putString(prefix + "_word", word)
        outState.putInt(prefix + "_number", number)
        outState.putString(prefix + "_team", team.toString())
    }

    fun onRestoreInstanceState(inState: Bundle, prefix: String){
        word = inState.getString(prefix + "_word")
        number = inState.getInt(prefix + "_number")
        team = Team.valueOf(inState.getString(prefix + "_team"))
    }
}