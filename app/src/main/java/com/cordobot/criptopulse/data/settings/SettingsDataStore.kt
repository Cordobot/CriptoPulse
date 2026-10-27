package com.cordobot.criptopulse.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cordobot.criptopulse.domain.model.FiatCurrency
import com.cordobot.criptopulse.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** User preferences and sync metadata, kept apart from the market cache in Room. */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val currencyKey = stringPreferencesKey("currency")
    private val lastUpdatedKey = longPreferencesKey("last_updated")

    override val currency: Flow<FiatCurrency> =
        context.dataStore.data.map { FiatCurrency.fromApiCode(it[currencyKey]) }.distinctUntilChanged()

    override val lastUpdated: Flow<Long?> = context.dataStore.data.map { it[lastUpdatedKey] }

    override suspend fun setCurrency(currency: FiatCurrency) {
        context.dataStore.edit { it[currencyKey] = currency.apiCode }
    }

    override suspend fun setLastUpdated(millis: Long) {
        context.dataStore.edit { it[lastUpdatedKey] = millis }
    }
}
