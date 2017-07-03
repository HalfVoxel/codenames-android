package com.gullesnuffs.codenames

import android.util.Log
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.android.volley.RequestQueue
import com.android.volley.Response
import org.json.JSONObject
import java.net.URI
import kotlin.coroutines.experimental.EmptyCoroutineContext.plus


class Bot(val board: Board) {

    fun getClue(team: Team, requestQueue: RequestQueue, clueList: ClueList?, onFinish: (Clue) -> Any) {
        val words = board.words.flatten().filter { !it.contacted.value }.toTypedArray()

        var colorsString = words.joinToString(separator = "", transform = { w -> w.getColorCode() })
        var wordsString = words.joinToString(separator = ",", transform = { w -> w.word.value.toLowerCase() })
        var hintedString = ""
        if (clueList != null) {
            val hintedWords = mutableSetOf<String>()
            for (clue in clueList.list) {
                for (word in clue.getTargetWords()){
                    hintedWords.add(word)
                }
            }
            for (word in hintedWords){
                if(hintedString.length > 0)
                    hintedString += ","
                hintedString += word
            }
        }

        var query = "engine=glove"
        query += "&color=" + (if (team == Team.Red) "r" else "b")
        query += "&colors=" + colorsString
        query += "&words=" + wordsString
        query += "&index=0"
        query += "&count=10"
        if(hintedString.isEmpty())
            hintedString = "none"
        query += "&hinted_words=" + hintedString
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