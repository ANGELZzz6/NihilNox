package com.example.colorblend.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object ApiKeysManager {

    private const val PREFS_NAME = "colorblend_api_keys"

    const val KEY_GEMINI         = "gemini_api_key"
    const val KEY_GROQ           = "groq_api_key"
    const val KEY_IGDB_CLIENT_ID = "igdb_client_id"
    const val KEY_IGDB_TOKEN     = "igdb_access_token"
    const val KEY_GIANTBOMB      = "giantbomb_api_key"
    const val KEY_SERVIDOR_URL   = "servidor_url"
    const val KEY_SERVIDOR_KEY   = "servidor_key"

    private val KEYS_REQUERIDAS = listOf(KEY_GEMINI, KEY_GROQ)

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context, PREFS_NAME, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        if (prefs == null) init(context)
        return prefs!!
    }

    fun estanConfiguradas(context: Context): Boolean =
        KEYS_REQUERIDAS.all { key ->
            !getPrefs(context).getString(key, "").isNullOrBlank()
        }

    fun keysFaltantes(context: Context): List<String> {
        val nombres = mapOf(
            KEY_GEMINI to "Gemini API Key",
            KEY_GROQ   to "Groq API Key"
        )
        return KEYS_REQUERIDAS
            .filter { getPrefs(context).getString(it, "").isNullOrBlank() }
            .mapNotNull { nombres[it] }
    }

    fun getGeminiKey(context: Context)    = getPrefs(context).getString(KEY_GEMINI, "") ?: ""
    fun getGroqKey(context: Context)      = getPrefs(context).getString(KEY_GROQ, "") ?: ""
    fun getIgdbClientId(context: Context) = getPrefs(context).getString(KEY_IGDB_CLIENT_ID, "") ?: ""
    fun getIgdbToken(context: Context)    = getPrefs(context).getString(KEY_IGDB_TOKEN, "") ?: ""
    fun getGiantBombKey(context: Context) = getPrefs(context).getString(KEY_GIANTBOMB, "") ?: ""
    fun getServidorUrl(context: Context)  = getPrefs(context).getString(KEY_SERVIDOR_URL, "") ?: ""
    fun getServidorKey(context: Context)  = getPrefs(context).getString(KEY_SERVIDOR_KEY, "") ?: ""

    fun set(context: Context, key: String, value: String) {
        getPrefs(context).edit().putString(key, value.trim()).apply()
    }

    fun get(context: Context, key: String): String =
        getPrefs(context).getString(key, "") ?: ""

    fun clear(context: Context, key: String) {
        getPrefs(context).edit().remove(key).apply()
    }
}