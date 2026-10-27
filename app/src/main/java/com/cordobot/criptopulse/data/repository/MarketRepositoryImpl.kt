package com.cordobot.criptopulse.data.repository

import com.cordobot.criptopulse.data.local.CoinEntity
import com.cordobot.criptopulse.data.local.CoinWithFavorite
import com.cordobot.criptopulse.data.local.FavoriteEntity
import com.cordobot.criptopulse.data.local.MarketDao
import com.cordobot.criptopulse.data.remote.CoinGeckoApi
import com.cordobot.criptopulse.data.remote.MarketCoinDto
import com.cordobot.criptopulse.domain.model.Coin
import com.cordobot.criptopulse.domain.model.FiatCurrency
import com.cordobot.criptopulse.domain.repository.MarketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketRepositoryImpl @Inject constructor(
    private val api: CoinGeckoApi,
    private val dao: MarketDao
) : MarketRepository {

    override fun observeCoins(): Flow<List<Coin>> = dao.observeCoins().map { list -> list.map { it.toDomain() } }

    override fun observeCoin(id: String): Flow<Coin?> = dao.observeCoin(id).map { it?.toDomain() }

    override suspend fun refresh(currency: FiatCurrency): Result<Unit> = try {
        val coins = api.getMarkets(currency.apiCode).mapNotNull { it.toEntity(currency) }
        dao.replaceCoins(coins)
        Result.success(Unit)
    } catch (e: HttpException) {
        Result.failure(Exception(if (e.code() == 429) "Límite de consultas de CoinGecko, reintentando pronto" else "Error del servidor (${e.code()})"))
    } catch (e: IOException) {
        Result.failure(Exception("Sin conexión"))
    }

    override suspend fun toggleFavorite(id: String) {
        if (dao.isFavorite(id)) dao.removeFavorite(id) else dao.addFavorite(FavoriteEntity(id))
    }
}

// Coins without price or rank (new listings) are skipped
internal fun MarketCoinDto.toEntity(currency: FiatCurrency): CoinEntity? {
    val price = currentPrice ?: return null
    val rank = marketCapRank ?: return null
    return CoinEntity(
        id = id,
        symbol = symbol.uppercase(),
        name = name,
        imageUrl = image,
        rank = rank,
        price = price,
        marketCap = marketCap ?: 0.0,
        volume24h = totalVolume ?: 0.0,
        high24h = high24h,
        low24h = low24h,
        change24h = change24h,
        change7d = change7d,
        circulatingSupply = circulatingSupply,
        allTimeHigh = ath,
        sparkline = sparkline?.price.orEmpty().joinToString(","),
        currency = currency.apiCode
    )
}

internal fun CoinWithFavorite.toDomain() = Coin(
    id = coin.id,
    symbol = coin.symbol,
    name = coin.name,
    imageUrl = coin.imageUrl,
    rank = coin.rank,
    price = coin.price,
    marketCap = coin.marketCap,
    volume24h = coin.volume24h,
    high24h = coin.high24h,
    low24h = coin.low24h,
    change24h = coin.change24h,
    change7d = coin.change7d,
    circulatingSupply = coin.circulatingSupply,
    allTimeHigh = coin.allTimeHigh,
    sparkline = coin.sparkline.split(',').mapNotNull { it.toDoubleOrNull() },
    currency = FiatCurrency.fromApiCode(coin.currency),
    isFavorite = isFavorite
)
