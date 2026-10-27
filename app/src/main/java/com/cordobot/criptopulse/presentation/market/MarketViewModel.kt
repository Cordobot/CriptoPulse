package com.cordobot.criptopulse.presentation.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cordobot.criptopulse.domain.model.Coin
import com.cordobot.criptopulse.domain.model.FiatCurrency
import com.cordobot.criptopulse.domain.model.MarketSort
import com.cordobot.criptopulse.domain.model.SyncStatus
import com.cordobot.criptopulse.domain.repository.MarketRepository
import com.cordobot.criptopulse.domain.repository.SettingsRepository
import com.cordobot.criptopulse.domain.usecase.FilterMarketUseCase
import com.cordobot.criptopulse.domain.usecase.RefreshMarketUseCase
import com.cordobot.criptopulse.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MarketUiState(
    val coins: List<Coin> = emptyList(),
    val hasCache: Boolean = false,
    val currency: FiatCurrency = FiatCurrency.USD,
    val query: String = "",
    val favoritesOnly: Boolean = false,
    val sort: MarketSort = MarketSort.RANK,
    val sync: SyncStatus = SyncStatus(lastUpdatedMillis = null)
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MarketViewModel @Inject constructor(
    market: MarketRepository,
    private val settings: SettingsRepository,
    private val filterMarket: FilterMarketUseCase,
    private val refreshMarket: RefreshMarketUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val favoritesOnly = MutableStateFlow(false)
    private val sort = MutableStateFlow(MarketSort.RANK)
    private val refreshing = MutableStateFlow(false)
    private val lastError = MutableStateFlow<String?>(null)

    // Polls the API while someone collects the state (the screen is visible) and restarts
    // right away when the currency changes; stateIn(WhileSubscribed) stops it in background.
    private val autoRefresh = settings.currency.flatMapLatest { currency ->
        flow {
            while (true) {
                refreshNow(currency)
                emit(Unit)
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    private val filters = combine(query, favoritesOnly, sort) { q, fav, s -> Triple(q, fav, s) }
    private val sync = combine(settings.lastUpdated, refreshing, lastError) { last, isRefreshing, error ->
        SyncStatus(last, isRefreshing, error)
    }

    val uiState: StateFlow<MarketUiState> = combine(
        market.observeCoins(),
        settings.currency,
        filters,
        sync,
        // The cache is shown right away, without waiting for the first download
        autoRefresh.onStart { emit(Unit) }
    ) { coins, currency, (q, fav, s), syncStatus, _ ->
        MarketUiState(
            coins = filterMarket(coins, q, fav, s),
            hasCache = coins.isNotEmpty(),
            currency = currency,
            query = q,
            favoritesOnly = fav,
            sort = s,
            sync = syncStatus
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MarketUiState())

    fun refresh() {
        viewModelScope.launch { refreshNow(settings.currency.first()) }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFavoritesOnlyChange(value: Boolean) {
        favoritesOnly.value = value
    }

    fun onSortChange(value: MarketSort) {
        sort.value = value
    }

    fun onCurrencyChange(currency: FiatCurrency) {
        viewModelScope.launch { settings.setCurrency(currency) }
    }

    fun onToggleFavorite(id: String) {
        viewModelScope.launch { toggleFavorite(id) }
    }

    private suspend fun refreshNow(currency: FiatCurrency) {
        refreshing.value = true
        lastError.value = refreshMarket(currency).exceptionOrNull()?.message
        refreshing.value = false
    }

    companion object {
        const val REFRESH_INTERVAL_MS = 60_000L
    }
}
