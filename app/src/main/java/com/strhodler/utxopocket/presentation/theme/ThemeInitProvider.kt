package com.strhodler.utxopocket.presentation.theme

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * Initializes the app night mode before any Activity is created so the splash
 * can respect the user's theme preference even on cold start.
 */
class ThemeInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val context = context ?: return true
        ThemeNightModeBootstrap.apply(context)
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
