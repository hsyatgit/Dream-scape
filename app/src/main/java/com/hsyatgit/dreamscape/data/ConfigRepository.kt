package com.hsyatgit.dreamscape.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class ConfigRepository(context: Context) {
    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    fun load(): AppConfig {
        val raw = prefs.getString(KEY, null) ?: return AppConfig()
        return runCatching { ConfigCodec.fromJson(raw) }.getOrElse { AppConfig() }
    }

    fun save(config: AppConfig) {
        prefs.edit().putString(KEY, ConfigCodec.toJson(config, includeSecrets = true)).apply()
    }

    private fun createPrefs(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            FILE,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private companion object {
        const val FILE = "dreamscape_secure_config"
        const val KEY = "config_json"
    }
}
