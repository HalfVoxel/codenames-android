package com.gullesnuffs.codenames

import android.app.Activity
import android.os.Bundle
import android.view.Window

class CameraActivity : Activity() {

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val cameraView = CameraView(this)
        cameraView.requestCode = getIntent().extras["RequestCode"] as RequestCode
        setContentView(cameraView)
    }
}
