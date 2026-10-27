package com.cordobot.criptopulse.domain.model

data class Coin(
    val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val rank: Int,
    val price: Double,
    val marketCap: Double,
    val volume24h: Double,
    val high24h: Double?,
    val low24h: Double?,
    val change24h: Double?,
    val change7d: Double?,
    val circulatingSupply: Double?,
    val allTimeHigh: Double?,
    // Hourly prices of the last 7 days, oldest first
    val sparkline: List<Double>,
    val currency: FiatCurrency,
    val isFavorite: Boolean
)

/** Currencies the prices can be shown in; [apiCode] is what CoinGecko expects. */
enum class FiatCurrency(val apiCode: String, val label: String, val languageTag: String) {
    USD("usd", "Dólar (USD)", "en-US"),
    EUR("eur", "Euro (EUR)", "es-ES"),
    COP("cop", "Peso colombiano (COP)", "es-CO"),
    MXN("mxn", "Peso mexicano (MXN)", "es-MX");

    companion object {
        fun fromApiCode(code: String?): FiatCurrency = entries.firstOrNull { it.apiCode == code } ?: USD
    }
}

enum class MarketSort(val label: String) {
    RANK("Capitalización"),
    GAINERS("Mayores alzas"),
    LOSERS("Mayores bajas")
}

/** Outcome of the last sync, so the UI can say whether the data is live or from the cache. */
data class SyncStatus(
    val lastUpdatedMillis: Long?,
    val isRefreshing: Boolean = false,
    val error: String? = null
)
