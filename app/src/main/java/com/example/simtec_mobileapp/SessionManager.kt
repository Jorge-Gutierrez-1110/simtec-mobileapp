package com.example.simtec_mobileapp

import android.content.Context

class SessionManager(context: Context) {

    private val prefs =
        context.getSharedPreferences("simtec_session", Context.MODE_PRIVATE)

    fun saveLogin(user: String) {

        prefs.edit()
            .putBoolean("logged", true)
            .putString("user", user)
            .apply()
    }

    fun isLogged(): Boolean {

        return prefs.getBoolean("logged", false)
    }

    fun logout() {

        prefs.edit().clear().apply()
    }
}