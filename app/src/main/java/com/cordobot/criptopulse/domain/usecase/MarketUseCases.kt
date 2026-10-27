package com.cordobot.criptopulse.domain.usecase

import com.cordobot.criptopulse.domain.model.Coin
import com.cordobot.criptopulse.domain.model.FiatCurrency
import com.cordobot.criptopulse.domain.model.MarketSort
import com.cordobot.criptopulse.domain.repository.MarketRepository
import com.cordobot.criptopulse.domain.repository.SettingsRepository
import javax.inject.Inject

class FilterMarketUseCase @Inject constructor() {
    operator fun invoke(coins: List<Coin>, query: String, favoritesOnly: Boolean, sort: MarketSort): List<Coin> {
        val text = query.trim()
        val filtered = coins.filter { coin ->
            (!favoritesOnly || coin.isFavorite) &&
                (text.isEmpty() || coin.name.contains(text, ignoreCase = true) || coin.symbol.contains(text, ignoreCase = true))
        }
        return when (sort) {
            MarketSort.RANK -> filtered.sortedBy { it.rank }
            MarketSort.GAINERS -> filtered.sortedByDescending { it.change24h ?: Double.NEGATIVE_INFINITY }
            MarketSort.LOSERS -> filtered.sortedBy { it.change24h ?: Double.POSITIVE_INFINITY }
        }
    }
}

/** Refreshes the market and records when it last succeeded. */
class RefreshMarketUseCase @Inject constructor(
    private val market: MarketRepository,
    private val settings: SettingsRepository
) {
    suspend operator fun invoke(currency: FiatCurrency, now: () -> Long = System::currentTimeMillis): Result<Unit> =
        market.refresh(currency).onSuccess { settings.setLastUpdated(now()) }
}

class ToggleFavoriteUseCase @Inject constructor(private val market: MarketRepository) {
    suspend operator fun invoke(id: String) = market.toggleFavorite(id)
}
