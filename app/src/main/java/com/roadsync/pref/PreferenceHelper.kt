package com.roadsync.pref

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.roadsync.auth.model.User

class PreferenceHelper {
    companion object {

        private val helper = PreferenceHelper()
        private lateinit var preferences: SharedPreferences

        fun getPref(context: Context): PreferenceHelper {
            preferences =
                context.getSharedPreferences(PreferenceKeys.KEY_PREF_NAME, Context.MODE_PRIVATE)
            return helper
        }
    }

    fun getPreferences(): PreferenceHelper {
        val helper = PreferenceHelper()
        return helper
    }


    fun isUserLogin(): Boolean {
        return preferences.getBoolean(PreferenceKeys.KEY_PREF_IS_LOGIN, false)
    }

    fun setUserLogin(value: Boolean) {
        preferences.edit().putBoolean(PreferenceKeys.KEY_PREF_IS_LOGIN, value).apply()
    }




    fun saveCurrentUser(user: User?) {
        val gson = Gson()
        val json = gson.toJson(user)
        preferences.edit().putString(PreferenceKeys.KEY_PREF_USER, json).apply()

    }

    fun getCurrentUser(): User? {
        val gson = Gson()
        val json = preferences.getString(PreferenceKeys.KEY_PREF_USER, null)
        json?.let {
            return gson.fromJson(json, User::class.java)
        }
        return null
    }

    fun saveBooleanValue(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }


    fun getBooleanValue(key: String, b: Boolean): Boolean {
        return preferences.getBoolean(key, false)
    }


    fun saveIntValue(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    fun saveStringValue(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    fun getStringValue(key: String, defaultValue: String? = null): String? {
        return preferences.getString(key, defaultValue)
    }

    fun saveLongValue(key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
    }

    fun getLongValue(key: String, defaultValue: Long = 0L): Long {
        return preferences.getLong(key, defaultValue)
    }

    fun getIntValue(key: String, defaultValue: Int = 0): Int {
        return preferences.getInt(key, defaultValue)
    }

    fun clearPreferences() {
        preferences.edit().clear().apply()
    }
}