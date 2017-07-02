package com.gullesnuffs.codenames

import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceActivity
import android.preference.ListPreference
import android.preference.Preference
import android.content.SharedPreferences




class SettingsActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }

    class MyPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
        }
    }

}
