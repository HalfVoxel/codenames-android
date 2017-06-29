package com.gullesnuffs.codenames

import android.util.Log
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.android.volley.RequestQueue
import com.android.volley.Response
import org.json.JSONObject


class Bot() {
    val words = mutableListOf<Word>()

    fun init(board : Board){
        words.clear()
        for(row in board.words){
            for(word in row){
                if(!word.contacted) {
                    words.add(word)
                }
            }
        }
    }

    fun getClue(team : Team, requestQueue: RequestQueue, onFinish: (Clue) -> Any) {
        var colorsString = ""
        var wordsString = ""
        for(word in words){
            colorsString += word.getColorCode()
            if(wordsString.length > 0)
                wordsString += ","
            wordsString += word.word.toLowerCase()
        }

        var url = "https://judge.omogenheap.se/codenames/api/1/clue?"
        url += "engine=glove"
        url += "&color=" + (if(team == Team.Red) "r" else "b")
        url += "&colors=" + colorsString
        url += "&words=" + wordsString
        url += "&index=0"
        url += "&count=10"

        val stringRequest = StringRequest(Request.Method.GET, url,
            object : Response.Listener<String> {
                override fun onResponse(response: String) {
                    val result = JSONObject(response)
                    val status = result.getString("status")
                    val message = result.getString("message")
                    val bestClues = result.getJSONArray("result")
                    var bestClue : Clue? = null
                    for(i in 0 until bestClues.length()){
                        val jsonClue = bestClues.getJSONObject(i)
                        val word = jsonClue.getString("word")
                        val count = jsonClue.getInt("count")
                        if(i == 0){
                            bestClue = Clue(word, count, team)
                        }
                        val why = jsonClue.getJSONArray("why")
                        for(j in 0 until why.length()){
                            val jsonWhy = why.getJSONObject(j)
                            val whyScore = jsonWhy.getDouble("score")
                            val whyWord = jsonWhy.getString("word")
                            val whyType = jsonWhy.getString("type")
                        }
                    }
                    onFinish(bestClue!!)
                }
            }, object : Response.ErrorListener {
                override fun onErrorResponse(error: VolleyError) {
                    Log.d("Response", "That didn't work!")
                }
            }
        )

        requestQueue.add(stringRequest)
    }
}