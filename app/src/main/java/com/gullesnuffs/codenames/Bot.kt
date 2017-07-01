package com.gullesnuffs.codenames

import android.util.Log
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.android.volley.RequestQueue
import com.android.volley.Response
import org.json.JSONObject


class Bot(val board: Board) {

    fun getClue(team: Team, requestQueue: RequestQueue, clueList: ClueList?, onFinish: (Clue) -> Any) {
        val words = board.words.flatten().filter { !it.contacted }.toTypedArray()

        var colorsString = words.joinToString(separator = "", transform = { w -> w.getColorCode() })
        var wordsString = words.joinToString(separator = ",", transform = { w -> w.word.toLowerCase() })
        if(clueList != null){
            for(clue in clueList.list){
                if(clue.team == team){
                    colorsString += "a"
                    wordsString += "," + clue.word.toLowerCase()
                }
            }
        }

        var url = "https://judge.omogenheap.se/codenames/api/1/clue?"
        url += "engine=glove"
        url += "&color=" + (if (team == Team.Red) "r" else "b")
        url += "&colors=" + colorsString
        url += "&words=" + wordsString
        url += "&index=0"
        url += "&count=10"

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
                            if (i == 0) {
                                bestClue = Clue(word, count, team)
                            }
                            val why = jsonClue.getJSONArray("why")
                            for (j in 0 until why.length()) {
                                val jsonWhy = why.getJSONObject(j)
                                val whyScore = jsonWhy.getDouble("score")
                                val whyWord = jsonWhy.getString("word")
                                val whyType = jsonWhy.getString("type")
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