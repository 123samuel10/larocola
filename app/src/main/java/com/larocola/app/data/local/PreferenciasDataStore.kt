package com.larocola.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.larocola.app.core.config.AppConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "preferencias")

class PreferenciasDataStore(private val context: Context) {

    private val SLUG_KEY = stringPreferencesKey("slug_establecimiento")

    val slug: Flow<String> = context.dataStore.data
        .map { it[SLUG_KEY] ?: AppConfig.defaultSlug }

    suspend fun guardarSlug(slug: String) {
        context.dataStore.edit { it[SLUG_KEY] = slug }
    }
}
