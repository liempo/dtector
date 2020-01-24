package com.liempo.drowsy.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.liempo.drowsy.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preference, rootKey)

        findPreference<Preference>("pref_back")?.setOnPreferenceClickListener {
            activity?.onBackPressed()
            true
        }

    }


}