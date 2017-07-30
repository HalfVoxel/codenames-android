package com.gullesnuffs.codenames

import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment


class SettingsActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }

}
