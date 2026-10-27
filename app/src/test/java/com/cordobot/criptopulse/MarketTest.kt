package com.cordobot.criptopulse

import com.cordobot.criptopulse.data.local.CoinWithFavorite
import com.cordobot.criptopulse.data.remote.MarketCoinDto
import com.cordobot.criptopulse.data.remote.SparklineDto
import com.cordobot.criptopulse.data.repository.toDomain
import com.cordobot.criptopulse.data.repository.toEntity
import com.cordobot.criptopulse.domain.model.Coin
import com.cordobot.criptopulse.domain.model.FiatCurrency
import com.cordobot.criptopulse.domain.model.MarketSort
import com.cordobot.criptopulse.domain.repository.MarketRepository
import com.cordobot.criptopulse.domain.repository.SettingsRepository
import com.cordobot.criptopulse.domain.usecase.FilterMarketUseCase
import com.cordobot.criptopulse.domain.usecase.RefreshMarketUseCase
import com.cordobot.criptopulse.presentation.util.formatChange
import com.cordobot.criptopulse.presentation.util.relativeTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarketTest {

    private fun coin(id: String, rank: Int, change: Double?, favorite: Boolean = false) = Coin(
        id = id, symbol = id.uppercase(), name = id.replaceFirstChar { it.uppercase() }, imageUrl = "",
        rank = rank, price = 1.0, marketCap = 0.0, volume24h = 0.0, high24h = null, low24h = null,
        change24h = change, change7d = null, circulatingSupply = null, allTimeHigh = null,
        sparkline = emptyList(), currency = FiatCurrency.USD, isFavorite = favorite
    )

    private val coins = listOf(
        coin("bitcoin", 1, -0.3, favorite = true),
        coin("ethereum", 2, 4.1),
        coin("solana", 5, -7.2, favorite = true),
        coin("newcoin", 90, null)
    )
    private val filter = FilterMarketUseCase()

    @Test
    fun `sorts by gainers and losers leaving coins without change last`() {
        assertEquals(listOf("ethereum", "bitcoin", "solana", "newcoin"), filter(coins, "", false, MarketSort.GAINERS).map { it.id })
        assertEquals(listOf("solana", "bitcoin", "ethereum", "newcoin"), filter(coins, "", false, MarketSort.LOSERS).map { it.id })
    }

    @Test
    fun `filters favorites and searches by name or symbol`() {
        assertEquals(listOf("bitcoin", "solana"), filter(coins, "", true, MarketSort.RANK).map { it.id })
        assertEquals(listOf("ethereum"), filter(coins, "ETH", false, MarketSort.RANK).map { it.id })
    }

    @Test
    fun `coins without price are skipped and the sparkline survives the cache`() {
        val dto = MarketCoinDto(
            id = "bitcoin", symbol = "btc", name = "Bitcoin", image = "img", marketCapRank = 1,
            currentPrice = 79876.0, sparkline = SparklineDto(listOf(1.5, 2.0, 3.25))
        )
        val entity = dto.toEntity(FiatCurrency.EUR)!!
        val restored = CoinWithFavorite(entity, isFavorite = true).toDomain()

        assertEquals("BTC", restored.symbol)
        assertEquals(listOf(1.5, 2.0, 3.25), restored.sparkline)
        assertEquals(FiatCurrency.EUR, restored.currency)
        assertNull(dto.copy(currentPrice = null).toEntity(FiatCurrency.USD))
    }

    @Test
    fun `refresh records the sync time only when it succeeds`() = runTest {
        val settings = FakeSettings()
        val market = FakeMarket(Result.failure(Exception("Sin conexión")))
        val refresh = RefreshMarketUseCase(market, settings)

        refresh(FiatCurrency.USD) { 1_000L }
        assertNull(settings.last.value)

        market.result = Result.success(Unit)
        refresh(FiatCurrency.COP) { 2_000L }
        assertEquals(2_000L, settings.last.value)
        assertEquals(FiatCurrency.COP, market.lastCurrency)
    }

    @Test
    fun `formats changes and relative times in spanish`() {
        assertEquals("+4,10 %", formatChange(4.1))
        assertEquals("-0,30 %", formatChange(-0.3))
        assertEquals("hace 5 min", relativeTime(0, 5 * 60_000))
        assertEquals("hace 2 h", relativeTime(0, 2 * 3_600_000))
    }

    private class FakeMarket(var result: Result<Unit>) : MarketRepository {
        var lastCurrency: FiatCurrency? = null
        override fun observeCoins(): Flow<List<Coin>> = flowOf(emptyList())
        override fun observeCoin(id: String): Flow<Coin?> = flowOf(null)
        override suspend fun refresh(currency: FiatCurrency): Result<Unit> = result.also { lastCurrency = currency }
        override suspend fun toggleFavorite(id: String) = Unit
    }

    private class FakeSettings : SettingsRepository {
        val last = MutableStateFlow<Long?>(null)
        override val currency: Flow<FiatCurrency> = flowOf(FiatCurrency.USD)
        override val lastUpdated: Flow<Long?> = last
        override suspend fun setCurrency(currency: FiatCurrency) = Unit
        override suspend fun setLastUpdated(millis: Long) { last.value = millis }
    }
}
