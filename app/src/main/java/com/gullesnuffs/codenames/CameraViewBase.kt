package com.gullesnuffs.codenames

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.IOException
import java.util.concurrent.Semaphore

abstract class CameraViewBase(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var mCamera: Camera? = null
    private val mHolder: SurfaceHolder = holder
    var frameWidth: Int = 0
        private set
    var frameHeight: Int = 0
        private set
    private var mFrame: ByteArray? = null
    private var mThreadRun: Boolean = false
    private val frameAvailable = Semaphore(1)

    init {
        mHolder.addCallback(this)
    }

    override fun surfaceChanged(_holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.i(TAG, "surfaceChanged")
        if (mCamera != null) {
            val params = mCamera!!.parameters
            val sizes = params.supportedPreviewSizes
            frameWidth = width
            frameHeight = height

            // Select optimal camera preview size
            var minDiff = java.lang.Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - height) < minDiff) {
                    frameWidth = size.width
                    frameHeight = size.height
                    minDiff = Math.abs(size.height - height).toDouble()
                }
            }

            params.setPreviewSize(frameWidth, frameHeight)
            params.focusMode = FOCUS_MODE_CONTINUOUS_PICTURE
            mCamera!!.setDisplayOrientation(90)
            mCamera!!.parameters = params
            try {
                mCamera!!.setPreviewDisplay(mHolder)
                //mCamera.setPreviewTexture(surfaceTexture);
            } catch (e: IOException) {
                Log.e(TAG, "mCamera.setPreviewDisplay fails: " + e)
            }

            mCamera!!.startPreview()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceCreated")
        mCamera = Camera.open()
        mCamera!!.setPreviewCallback { data, camera ->
            synchronized(this@CameraViewBase) {
                mFrame = data
                frameAvailable.release()
            }
        }
        Thread(this).start()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceDestroyed")
        mThreadRun = false
        frameAvailable.release()
        if (mCamera != null) {
            synchronized(this) {
                mCamera!!.stopPreview()
                mCamera!!.setPreviewCallback(null)
                mCamera!!.release()
                mCamera = null
            }
        }
    }

    protected abstract fun processFrame(data: ByteArray)

    override fun run() {
        mThreadRun = true
        while (mThreadRun) {
            frameAvailable.acquire()
            synchronized(this) {
                try {
                    val frame = mFrame
                    if (frame != null) {
                        processFrame(frame)
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        }
    }

    companion object {
        private val TAG = "Sample::SurfaceView"
    }
}