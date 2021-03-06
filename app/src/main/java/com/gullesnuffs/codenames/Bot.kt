package com.gullesnuffs.codenames

import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject
import java.net.URI


class Bot(val board: Board) {

    fun getClue(team: Team, requestQueue: RequestQueue, clueList: ClueList?, difficulty: String, inappropriateMode: String, onFinish: (Clue) -> Any) {
        val words = board.words.flatten().filter { !it.contacted.value }.toTypedArray()

        val colorsString = words.joinToString(separator = "", transform = { w -> w.getColorCode() })
        val wordsString = words.joinToString(separator = ",", transform = { w -> w.word.value.toLowerCase() })
        var hintedString = ""
        var oldClueString = ""
        if (clueList != null) {
            val hintedWords = mutableSetOf<String>()
            for (clue in clueList.list) {
                hintedWords += clue.getTargetWords()
                if(oldClueString.length > 0)
                    oldClueString += ","
                oldClueString += clue.word
            }
            for (word in hintedWords){
                if(hintedString.length > 0)
                    hintedString += ","
                hintedString += word
            }
        }

        var engine = "glove"

        // Glove only contains single words, so we have to use a different engine if any of
        // the words are actually two words
        for(word in words){
            if(" " in word.word.value){
                engine = "conceptnet"
            }
        }

        var query = "engine=" + engine
        query += "&color=" + (if (team == Team.Red) "r" else "b")
        query += "&colors=" + colorsString
        query += "&words=" + wordsString
        query += "&index=0"
        query += "&count=10"
        if(hintedString.isEmpty())
            hintedString = "no_words"
        query += "&hinted_words=" + hintedString
        if(oldClueString.isEmpty())
            oldClueString = "no_words"
        query += "&old_clues=" + oldClueString
        query += "&inappropriate=" + inappropriateMode
        query += "&difficulty=" + difficulty
        val url = URI("https", "judge.omogenheap.se", "/codenames/api/1/clue", query, "").toASCIIString()

        val stringRequest = StringRequest(Request.Method.GET, url,
                {
                    response ->
                    try {
                        val result = JSONObject(response)
                        val status = result.getString("status")
                        val message = result.getString("message")
                        val bestClues = result.getJSONArray("result")
                        var bestClue: Clue? = null
                        for (i in 0 until bestClues.length()) {
                            val jsonClue = bestClues.getJSONObject(i)
                            val word = jsonClue.getString("word")
                            val count = jsonClue.getInt("count")
                            val clue = Clue(word, count, team)
                            clue.explanation = Explanation()
                            val why = jsonClue.getJSONArray("why")
                            for (j in 0 until why.length()) {
                                val jsonWhy = why.getJSONObject(j)
                                val whyWord = jsonWhy.getString("word")
                                val whyScore = jsonWhy.getDouble("score")
                                val whyType = jsonWhy.getString("type")
                                clue.explanation!!.add(whyWord, whyScore, whyType)
                            }
                            if (i == 0) {
                                bestClue = clue
                            }
                        }
                        onFinish(bestClue!!)
                    } catch (e: Exception) {
                        Log.v("JSON", response)
                        Log.e("JSON", e.toString())
                    }
                },
                {
                    error ->
                    Log.d("Response", "That didn't work!\n" + error.toString())
                }
        )

        requestQueue.add(stringRequest)
    }
}