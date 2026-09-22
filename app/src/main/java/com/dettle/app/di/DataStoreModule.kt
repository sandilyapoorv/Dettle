package com.dettle.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.dettle.app.orchestrator.mode.ModeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

// Extension property — creates a single DataStore instance per app process
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dettle_prefs")

/**
 * Separate Hilt module for DataStore.
 *
 * DataStore's extension property pattern requires a `val` at the file level — it can't
 * be wired through a regular `@Provides` function in the same object as other providers.
 * This module isolates that and provides everything that depends on DataStore.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore

    @Provides
    @Singleton
    fun provideModeRepository(
        dataStore: DataStore<Preferences>,
        json: Json
    ): ModeRepository = ModeRepository(dataStore, json)
}
