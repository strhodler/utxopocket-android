package com.strhodler.utxopocket.presentation.theme

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.stringPreferencesKey
import com.strhodler.utxopocket.data.preferences.userPreferencesDataStore
import com.strhodler.utxopocket.domain.model.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Initializes the app night mode before any Activity is created so the splash
 * can respect the user's theme preference even on cold start.
 */
class ThemeInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val context = context ?: return true
        val themePreference = runBlocking {
            val key = stringPreferencesKey("theme_preference")
            context.userPreferencesDataStore.data.map { prefs ->
                prefs[key]?.let { stored ->
                    runCatching { ThemePreference.valueOf(stored) }.getOrNull()
                } ?: ThemePreference.SYSTEM
            }.first()
        }
        val nightMode = when (themePreference) {
            ThemePreference.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemePreference.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemePreference.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
