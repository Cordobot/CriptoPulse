package com.cordobot.criptopulse.di

import android.content.Context
import androidx.room.Room
import com.cordobot.criptopulse.BuildConfig
import com.cordobot.criptopulse.data.local.MarketDao
import com.cordobot.criptopulse.data.local.MarketDatabase
import com.cordobot.criptopulse.data.remote.CoinGeckoApi
import com.cordobot.criptopulse.data.repository.MarketRepositoryImpl
import com.cordobot.criptopulse.data.settings.SettingsDataStore
import com.cordobot.criptopulse.domain.repository.MarketRepository
import com.cordobot.criptopulse.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApi(): CoinGeckoApi {
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            })
            .build()
        return Retrofit.Builder()
            .baseUrl(CoinGeckoApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CoinGeckoApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MarketDatabase =
        Room.databaseBuilder(context, MarketDatabase::class.java, "criptopulse.db").build()

    @Provides
    fun provideDao(database: MarketDatabase): MarketDao = database.marketDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds
    abstract fun bindMarketRepository(impl: MarketRepositoryImpl): MarketRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsDataStore): SettingsRepository
}
