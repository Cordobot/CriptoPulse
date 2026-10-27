package com.cordobot.criptopulse.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "coins")
data class CoinEntity(
    @PrimaryKey val id: String,
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
    // Comma separated prices; a chart of 168 points does not need its own table
    val sparkline: String,
    val currency: String
)

/** Favorites live in their own table so a refresh of the market never erases them. */
@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val coinId: String)

data class CoinWithFavorite(
    @Embedded val coin: CoinEntity,
    val isFavorite: Boolean
)

@Dao
interface MarketDao {
    @Query(
        "SELECT coins.*, EXISTS(SELECT 1 FROM favorites WHERE favorites.coinId = coins.id) AS isFavorite " +
            "FROM coins ORDER BY rank"
    )
    fun observeCoins(): Flow<List<CoinWithFavorite>>

    @Query(
        "SELECT coins.*, EXISTS(SELECT 1 FROM favorites WHERE favorites.coinId = coins.id) AS isFavorite " +
            "FROM coins WHERE id = :id"
    )
    fun observeCoin(id: String): Flow<CoinWithFavorite?>

    @Query("DELETE FROM coins")
    suspend fun clearCoins()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoins(coins: List<CoinEntity>)

    @Transaction
    suspend fun replaceCoins(coins: List<CoinEntity>) {
        clearCoins()
        insertCoins(coins)
    }

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE coinId = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE coinId = :id")
    suspend fun removeFavorite(id: String)
}

@Database(entities = [CoinEntity::class, FavoriteEntity::class], version = 1, exportSchema = true)
abstract class MarketDatabase : RoomDatabase() {
    abstract fun marketDao(): MarketDao
}
