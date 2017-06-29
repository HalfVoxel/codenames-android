package com.gullesnuffs.codenames

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.Volley
import org.joda.time.DateTime
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream


internal class CameraView(context: Context) : CameraViewBase(context) {

    var previousRequestDone = true
    var lastUploadTime : DateTime = DateTime.now()
    val requestQueue : RequestQueue = Volley.newRequestQueue(context)
    var requestCode : RequestCode? = null

    override fun processFrame(data: ByteArray) {
        if (lastUploadTime.plusMillis(1000).isBeforeNow() && previousRequestDone) {
            lastUploadTime = DateTime.now()
            uploadFrame(data)
        }
    }

    fun uploadFrame(data: ByteArray) {
        // Alter the second parameter of this to the actual format you are receiving
        val yuv = YuvImage(data, ImageFormat.NV21, frameWidth, frameHeight, null)
        val jpeg = ByteArrayOutputStream()
        yuv.compressToJpeg(Rect(0, 0, frameWidth, frameHeight), 70, jpeg)
        val imageData = jpeg.toByteArray()

        val url =
                if(requestCode == RequestCode.WordRecognition)
                    "https://judge.omogenheap.se/codenames/api/1/ocr-board"
                else
                    "https://judge.omogenheap.se/codenames/api/1/ocr-grid"
        previousRequestDone = false
        val multipartRequest = object : VolleyMultipartRequest(Request.Method.POST, url, Response.Listener<NetworkResponse>
        {
            response ->
            previousRequestDone = true
            val resultResponse = String(response.data)
            Log.d("Result", resultResponse)
            try {
                val result = JSONObject(resultResponse)
                val status = result.getString("status")
                val message = result.getString("message")
                val grid = result.getJSONArray("grid")
                val words = ArrayList<ArrayList<String>>()
                for (r in 0 until grid.length()) {
                    val line = ArrayList<String>()
                    if(requestCode == RequestCode.WordRecognition) {
                        val jsonLine = grid.getJSONArray(r)
                        for (c in 0 until jsonLine.length()) {
                            line.add(jsonLine.getString(c))
                        }
                    }
                    else if(requestCode == RequestCode.GridRecognition){
                        val jsonLine = grid.getString(r)
                        for (c in 0 until jsonLine.length){
                            line.add(jsonLine[c].toString())
                        }
                    }
                    words.add(line)
                }

                if (status == "1") {
                    // tell everybody you have succed upload image and post strings
                    Log.i("Messsage", message)
                    Log.i("Grid: ", words.toString())
                    var wordCount = 0
                    var countA = 0
                    var countB = 0
                    var countC = 0
                    var countR = 0
                    for(row in words){
                        for(word in row){
                            if(word.length > 0)
                                wordCount += 1
                            when(word){
                                "a" -> countA++
                                "b" -> countB++
                                "c" -> countC++
                                "r" -> countR++
                            }
                        }
                    }
                    var failed = wordCount < 25
                    if(requestCode == RequestCode.GridRecognition && (countA == 0 || countC != 7 || countR < 8 || countB < 8)){
                        failed = true
                    }
                    if(!failed){
                        val data = Intent()
                        for(i in 0 until 5){
                            for(j in 0 until 5){
                                val key = "word" + i.toString() + "_" + j.toString()
                                data.putExtra(key, words[i][j])
                            }
                        }
                        data.setData(Uri.parse(""))
                        var activity = context as Activity
                        activity.setResult(RESULT_OK, data);
                        activity.finish()
                    }
                } else {
                    Log.i("Unexpected", message)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }, Response.ErrorListener { error ->
            previousRequestDone = true
            val networkResponse = error.networkResponse
            var errorMessage = "Unknown error"
            if (networkResponse == null) {
                if (error.javaClass == TimeoutError::class.java) {
                    errorMessage = "Request timeout"
                } else if (error.javaClass == NoConnectionError::class.java) {
                    errorMessage = "Failed to connect server"
                }
            }
            Log.i("Error", errorMessage)
            error.printStackTrace()
        }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String,String>()
                params.put("size", "5x5")
                return params
            }

            override fun getByteData(): Map<String, DataPart> {
                return mapOf("file" to DataPart("file.jpg", imageData, "image/jpeg"))
            }
        }

        requestQueue.add(multipartRequest)
    }
}