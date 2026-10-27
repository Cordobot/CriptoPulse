package com.cordobot.criptopulse.domain.repository

import com.cordobot.criptopulse.domain.model.Coin
import com.cordobot.criptopulse.domain.model.FiatCurrency
import kotlinx.coroutines.flow.Flow

interface MarketRepository {
    /** Cached market (single source of truth); emits again after every refresh or favorite change. */
    fun observeCoins(): Flow<List<Coin>>

    fun observeCoin(id: String): Flow<Coin?>

    /** Downloads the top coins in [currency] and replaces the cache. */
    suspend fun refresh(currency: FiatCurrency): Result<Unit>

    suspend fun toggleFavorite(id: String)
}

interface SettingsRepository {
    val currency: Flow<FiatCurrency>
    val lastUpdated: Flow<Long?>

    suspend fun setCurrency(currency: FiatCurrency)
    suspend fun setLastUpdated(millis: Long)
}
