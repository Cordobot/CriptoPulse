package com.cordobot.criptopulse.presentation.market

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cordobot.criptopulse.domain.model.Coin
import com.cordobot.criptopulse.domain.model.FiatCurrency
import com.cordobot.criptopulse.domain.model.MarketSort
import com.cordobot.criptopulse.domain.model.SyncStatus
import com.cordobot.criptopulse.presentation.components.ChangeChip
import com.cordobot.criptopulse.presentation.components.Sparkline
import com.cordobot.criptopulse.presentation.components.trendColor
import com.cordobot.criptopulse.presentation.detail.CoinDetailContent
import com.cordobot.criptopulse.presentation.theme.Down
import com.cordobot.criptopulse.presentation.theme.Up
import com.cordobot.criptopulse.presentation.util.formatPrice
import com.cordobot.criptopulse.presentation.util.relativeTime
import kotlinx.coroutines.delay

/**
 * Market list. On wide screens (tablets, foldables, landscape) the detail is shown next to the
 * list; on phones selecting a coin calls [onOpenCoin] to navigate.
 */
@Composable
fun MarketScreen(
    onOpenCoin: (String) -> Unit,
    viewModel: MarketViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        val twoPane = maxWidth >= 720.dp
        if (twoPane) {
            Row {
                MarketList(
                    state = state,
                    viewModel = viewModel,
                    onCoinClick = { selectedId = it },
                    modifier = Modifier.weight(1f)
                )
                val selected = state.coins.firstOrNull { it.id == selectedId } ?: state.coins.firstOrNull()
                Box(modifier = Modifier.weight(1.2f).fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    if (selected != null) {
                        CoinDetailContent(coin = selected, onToggleFavorite = { viewModel.onToggleFavorite(selected.id) })
                    }
                }
            }
        } else {
            MarketList(state = state, viewModel = viewModel, onCoinClick = onOpenCoin)
        }
    }
}

@Composable
private fun MarketList(
    state: MarketUiState,
    viewModel: MarketViewModel,
    onCoinClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopBar(
            sync = state.sync,
            currency = state.currency,
            onCurrencyChange = viewModel::onCurrencyChange,
            onRefresh = viewModel::refresh
        )
        AnimatedVisibility(visible = state.sync.error != null && state.hasCache) {
            OfflineBanner(state.sync)
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Buscar Bitcoin, ETH…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Filters(state, viewModel)

        when {
            !state.hasCache && state.sync.error != null -> EmptyError(state.sync.error, onRetry = viewModel::refresh)
            !state.hasCache -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.coins.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (state.favoritesOnly) "Aún no tienes favoritos. Toca la ★ en el detalle de una moneda."
                    else "Sin resultados",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(state.coins, key = { it.id }) { coin ->
                    CoinRow(coin, onClick = { onCoinClick(coin.id) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    sync: SyncStatus,
    currency: FiatCurrency,
    onCurrencyChange: (FiatCurrency) -> Unit,
    onRefresh: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    // Ticks every 30 s so "hace X min" stays current between refreshes
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000)
        }
    }

    Row(
        modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("CriptoPulse", style = MaterialTheme.typography.headlineLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val live = sync.error == null && sync.lastUpdatedMillis != null
                Box(
                    Modifier
                        .size(8.dp)
                        .background(if (live) Up else Down, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        sync.isRefreshing -> "Actualizando…"
                        sync.lastUpdatedMillis == null -> "Sin datos aún"
                        live -> "En vivo · ${relativeTime(sync.lastUpdatedMillis, now)}"
                        else -> "Datos guardados · ${relativeTime(sync.lastUpdatedMillis, now)}"
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box {
            TextButton(onClick = { menuOpen = true }) {
                Text(currency.apiCode.uppercase(), fontWeight = FontWeight.Bold)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                FiatCurrency.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            menuOpen = false
                            onCurrencyChange(option)
                        }
                    )
                }
            }
        }
        IconButton(onClick = onRefresh, enabled = !sync.isRefreshing) {
            Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
        }
    }
}

@Composable
private fun OfflineBanner(sync: SyncStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(Down.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CloudOff, contentDescription = null, tint = Down)
        Spacer(Modifier.width(10.dp))
        Text(
            "${sync.error}. Mostrando los últimos precios guardados; se reintentará cada minuto.",
            fontSize = 13.sp
        )
    }
}

@Composable
private fun Filters(state: MarketUiState, viewModel: MarketViewModel) {
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = Color.White
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = state.favoritesOnly,
                onClick = { viewModel.onFavoritesOnlyChange(!state.favoritesOnly) },
                label = { Text("Favoritos") },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = chipColors
            )
        }
        items(MarketSort.entries) { sort ->
            FilterChip(
                selected = state.sort == sort,
                onClick = { viewModel.onSortChange(sort) },
                label = { Text(sort.label) },
                colors = chipColors
            )
        }
    }
}

@Composable
private fun CoinRow(coin: Coin, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            coin.rank.toString(),
            modifier = Modifier.width(28.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        AsyncImage(
            model = coin.imageUrl,
            contentDescription = null,
            modifier = Modifier.size(36.dp).clip(CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(coin.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (coin.isFavorite) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Star, contentDescription = "Favorito", tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                }
            }
            Text(coin.symbol, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Sparkline(
            points = coin.sparkline,
            color = trendColor(coin.change7d),
            modifier = Modifier.width(64.dp).height(28.dp),
            strokeWidth = 2.5f
        )
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(112.dp)) {
            Text(formatPrice(coin.price, coin.currency), fontWeight = FontWeight.SemiBold, maxLines = 1, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            ChangeChip(coin.change24h, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("$message. Conéctate para descargar el mercado por primera vez.")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}
