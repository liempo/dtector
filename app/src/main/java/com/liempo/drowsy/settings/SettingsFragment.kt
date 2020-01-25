package com.liempo.drowsy.settings

import android.os.Bundle
import androidx.preference.DropDownPreference
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

        findPreference<DropDownPreference>("pref_alarm")?.apply {
            entries = arrayOf("Alarm 1", "Alarm 2", "Alarm 3")
            entryValues = arrayOf("alarm_1", "alarm_2", "alarm_3")
        }

    }


}