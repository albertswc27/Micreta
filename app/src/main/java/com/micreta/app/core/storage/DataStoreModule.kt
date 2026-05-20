package com.micreta.app.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** App-wide DataStore<Preferences> singleton. */
val Context.micretaDataStore: DataStore<Preferences> by preferencesDataStore(name = "micreta_settings")
