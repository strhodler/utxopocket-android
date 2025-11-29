package com.strhodler.utxopocket.data.preferences

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal const val USER_PREFERENCES_NAME = "user_preferences"

val Context.userPreferencesDataStore by preferencesDataStore(name = USER_PREFERENCES_NAME)
