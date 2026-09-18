package io.github.mobdev.data

import android.content.Context

data class Credentials(
    val name: String,
    val password: String,
)

class CredentialsStore(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun save(name: String, password: String) {
        preferences.edit()
            .putString(KEY_NAME, name)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun load(): Credentials? {
        val name = preferences.getString(KEY_NAME, null) ?: return null
        val password = preferences.getString(KEY_PASSWORD, null) ?: return null
        if (name.isBlank() || password.isBlank()) return null
        return Credentials(name, password)
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "chat_credentials"
        const val KEY_NAME = "name"
        const val KEY_PASSWORD = "password"
    }
}
