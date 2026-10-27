package com.cordobot.criptopulse.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** CoinGecko public API (no key required, ~30 requests per minute). */
interface CoinGeckoApi {

    @GET("coins/markets")
    suspend fun getMarkets(
        @Query("vs_currency") currency: String,
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = true,
        @Query("price_change_percentage") changes: String = "24h,7d"
    ): List<MarketCoinDto>

    companion object {
        const val BASE_URL = "https://api.coingecko.com/api/v3/"
    }
}

@Serializable
data class MarketCoinDto(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String,
    @SerialName("market_cap_rank") val marketCapRank: Int? = null,
    @SerialName("current_price") val currentPrice: Double? = null,
    @SerialName("market_cap") val marketCap: Double? = null,
    @SerialName("total_volume") val totalVolume: Double? = null,
    @SerialName("high_24h") val high24h: Double? = null,
    @SerialName("low_24h") val low24h: Double? = null,
    @SerialName("price_change_percentage_24h_in_currency") val change24h: Double? = null,
    @SerialName("price_change_percentage_7d_in_currency") val change7d: Double? = null,
    @SerialName("circulating_supply") val circulatingSupply: Double? = null,
    val ath: Double? = null,
    @SerialName("sparkline_in_7d") val sparkline: SparklineDto? = null
)

@Serializable
data class SparklineDto(val price: List<Double> = emptyList())
