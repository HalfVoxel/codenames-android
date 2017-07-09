package com.gullesnuffs.codenames

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.*
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
    var lastUploadTime: DateTime = DateTime.now()
    val requestQueue: RequestQueue = Volley.newRequestQueue(context)
    var requestCode: RequestCode? = null
    var savedWords: Array<Array<String>>? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (requestCode == RequestCode.WordRecognition) drawWordVisualization(canvas);
        if (requestCode == RequestCode.GridRecognition) drawGridVisualization(canvas);
    }

    fun drawGridVisualization (canvas: Canvas) {
        var clipRect = canvas.clipBounds
        canvas.rotate(0f, clipRect.exactCenterY(), clipRect.exactCenterY())
        clipRect = canvas.clipBounds
        val paint = Paint()

        val w = 5
        val h = 5
        var cellW = Math.min(clipRect.width() / h.toFloat(), clipRect.height() / w.toFloat())
        var cellH = cellW
        val scale = 0.7f
        cellH *= scale
        cellW *= scale
        
        for (r in 0 until w) {
            for (c in 0 until h) {
                val rf = RectF(0f, 0f, cellW, cellH)
                rf.offsetTo(clipRect.exactCenterX() + (c - (w-1)/2f)*cellW, clipRect.exactCenterY()+ (r - (h-1)/2f)*cellH)
                rf.offset(-cellW*0.5f, -cellH*0.5f)

                rf.inset(cellW/20f, cellH/20f)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f
                paint.color = Color.argb(255, 221, 209, 168)
                canvas.drawRoundRect(rf, 20f, 20f, paint)
            }
        }

        paint.style = Paint.Style.STROKE
        paint.color = Color.argb(255, 221, 209, 168)
        paint.strokeWidth = 15f
        val rf = RectF(0f, 0f, cellW, cellH)
        rf.offsetTo(clipRect.exactCenterX(), clipRect.exactCenterY() + (h/2f)*cellH + 20f)
        canvas.drawLine(rf.left - w*cellW/2f, rf.top, rf.left + w*cellW/2f, rf.top, paint)
    }

    fun drawWordVisualization (canvas: Canvas) {
        var clipRect = canvas.clipBounds
        canvas.rotate(90f, clipRect.exactCenterY(), clipRect.exactCenterY())
        clipRect = canvas.clipBounds
        val paint = Paint()

        val w = 5
        val h = 5
        var cellW = clipRect.width() / h.toFloat()
        var cellH = Math.max(cellW, clipRect.height() / w.toFloat()) * 0.65f
        val scale = 0.7f
        cellH *= scale
        cellW *= scale

        val words = savedWords
        for (r in 0 until w) {
            for (c in 0 until h) {
                val rf = RectF(0f, 0f, cellW, cellH)
                rf.offsetTo(clipRect.exactCenterX() + (c - (w-1)/2f)*cellW, clipRect.exactCenterY()+ (r - (h-1)/2f)*cellH)
                rf.offset(-cellW*0.5f, -cellH*0.5f)

                rf.inset(cellW/20f, cellH/20f)

                if (words != null && words[r][c] != "") {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.argb(180, 122, 216, 88)
                    canvas.drawRoundRect(rf, 20f, 20f, paint)
                    paint.strokeWidth = 10f
                } else {
                    paint.strokeWidth = 5f
                }

                paint.style = Paint.Style.STROKE
                paint.color = Color.argb(255, 221, 209, 168)
                canvas.drawRoundRect(rf, 20f, 20f, paint)

                paint.style = Paint.Style.FILL
                paint.color = Color.argb(50, 255, 255, 255)
                rf.inset(cellH*0.10f, cellH*0.10f)
                rf.top += rf.height() * 0.65f
                canvas.drawRect(rf, paint)
            }
        }
    }

    fun saveWords(words : Array<Array<String>>) {
        val wordCount = words.flatten().filter { it != "" }.size
        if (wordCount <= 12) return

        var saved = savedWords
        if (saved == null) {
            savedWords = words
            saved = words
        } else {
            for ((savedRow, row) in saved.zip(words)) {
                for (i in 0 until row.size) {
                    if (row[i] != "") savedRow[i] = row[i];
                }
            }
        }

        val wordCount2 = saved.flatten().filter { it != "" }.size
        if (wordCount2 == 25) {
            sendData(saved)
        }
    }

    fun saveGrid(words : Array<Array<String>>) {
        var countA = 0
        var countB = 0
        var countC = 0
        var countR = 0
        for (word in words.flatten()) {
            when (word) {
                "a" -> countA++
                "b" -> countB++
                "c" -> countC++
                "r" -> countR++
            }
        }

        if (countA == 1 && countC == 7 && countR >= 8 && countB >= 8) {
            sendData(rotateCCW(words))
        }
    }

    fun rotateCCW(words : Array<Array<String>>): Array<Array<String>> {
        val result = Array(words.size, { Array(words.size, { "" }) })
        for (r in 0 until words.size) {
            for (c in 0 until words.size) {
                val nr = c
                val nc = words.size - r - 1
                result[nr][nc] = words[r][c]
            }
        }
        return result
    }

    override fun processFrame(data: ByteArray) {
        if (lastUploadTime.plusMillis(1000).isBeforeNow() && previousRequestDone) {
            lastUploadTime = DateTime.now()
            uploadFrame(data)
        }

        handler.post {
            invalidate()
        }
    }

    fun uploadFrame(data: ByteArray) {
        // Alter the second parameter of this to the actual format you are receiving
        val yuv = YuvImage(data, ImageFormat.NV21, frameWidth, frameHeight, null)
        val jpeg = ByteArrayOutputStream()
        yuv.compressToJpeg(Rect(0, 0, frameWidth, frameHeight), 70, jpeg)
        val imageData = jpeg.toByteArray()

        val url =
                if (requestCode == RequestCode.WordRecognition)
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
                val words = Array(grid.length(), { Array(grid.length(), { "" }) })
                for (r in 0 until grid.length()) {
                    val line = ArrayList<String>()
                    if (requestCode == RequestCode.WordRecognition) {
                        val jsonLine = grid.getJSONArray(r)
                        if (jsonLine.length() != grid.length()) throw Exception("Non-square matrix received from server")
                        for (c in 0 until jsonLine.length()) {
                            words[r][c] = jsonLine.getString(c)
                        }
                    } else if (requestCode == RequestCode.GridRecognition) {
                        val jsonLine = grid.getString(r)
                        if (jsonLine.length != grid.length()) throw Exception("Non-square matrix received from server")
                        for (c in 0 until jsonLine.length) {
                            words[r][c] = jsonLine[c].toString()
                        }
                    }
                }

                if (status == "1") {
                    // tell everybody you have succeed upload image and post strings
                    Log.i("Message", message)
                    Log.i("Grid: ", words.toString())

                    if (requestCode == RequestCode.GridRecognition) saveGrid(words)
                    if (requestCode == RequestCode.WordRecognition) saveWords(words)
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
                val params = HashMap<String, String>()
                params.put("size", "5x5")
                params.put("lang", "eng")
                return params
            }

            override fun getByteData(): Map<String, DataPart> {
                return mapOf("file" to DataPart("file.jpg", imageData, "image/jpeg"))
            }
        }

        requestQueue.add(multipartRequest)
    }

    fun sendData(words: Array<Array<String>>) {
        val data = Intent()
        for (i in 0 until 5) {
            for (j in 0 until 5) {
                val key = "word" + i.toString() + "_" + j.toString()
                data.putExtra(key, words[i][j])
            }
        }
        data.setData(Uri.parse(""))
        var activity = context as Activity
        activity.setResult(RESULT_OK, data);
        activity.finish()
    }
}
