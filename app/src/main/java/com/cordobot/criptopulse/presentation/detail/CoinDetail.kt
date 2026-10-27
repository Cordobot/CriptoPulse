package com.cordobot.criptopulse.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.cordobot.criptopulse.domain.model.Coin
import com.cordobot.criptopulse.domain.repository.MarketRepository
import com.cordobot.criptopulse.domain.usecase.ToggleFavoriteUseCase
import com.cordobot.criptopulse.presentation.components.ChangeChip
import com.cordobot.criptopulse.presentation.components.Sparkline
import com.cordobot.criptopulse.presentation.components.trendColor
import com.cordobot.criptopulse.presentation.util.formatCompact
import com.cordobot.criptopulse.presentation.util.formatPrice
import com.cordobot.criptopulse.presentation.util.formatSupply
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: MarketRepository,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {
    private val coinId: String = checkNotNull(savedStateHandle["id"])

    // Reads the cache, so it keeps updating while the market screen refreshes it
    val coin: StateFlow<Coin?> = repository.observeCoin(coinId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onToggleFavorite() {
        viewModelScope.launch { toggleFavorite(coinId) }
    }
}

@Composable
fun CoinDetailScreen(onBack: () -> Unit, viewModel: CoinDetailViewModel = hiltViewModel()) {
    val coin by viewModel.coin.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        val current = coin
        if (current == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            CoinDetailContent(current, onToggleFavorite = viewModel::onToggleFavorite, onBack = onBack)
        }
    }
}

@Composable
fun CoinDetailContent(coin: Coin, onToggleFavorite: () -> Unit, onBack: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (coin.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (coin.isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                    tint = if (coin.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(coin.imageUrl, contentDescription = null, modifier = Modifier.size(44.dp).clip(CircleShape))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(coin.name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp))
                    Text("${coin.symbol} · Puesto #${coin.rank}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(formatPrice(coin.price, coin.currency), style = MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ChangeChip(coin.change24h)
                    Text("24 h", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    ChangeChip(coin.change7d)
                    Text("7 días", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }

            PriceChart(coin)

            val stats = listOfNotNull(
                "Capitalización" to formatCompact(coin.marketCap, coin.currency),
                "Volumen 24 h" to formatCompact(coin.volume24h, coin.currency),
                coin.high24h?.let { "Máximo 24 h" to formatPrice(it, coin.currency) },
                coin.low24h?.let { "Mínimo 24 h" to formatPrice(it, coin.currency) },
                coin.allTimeHigh?.let { "Máximo histórico" to formatPrice(it, coin.currency) },
                coin.circulatingSupply?.let { "En circulación" to formatSupply(it, coin.symbol) }
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                stats.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (label, value) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(value, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Text(
                "Datos de CoinGecko. No es asesoría financiera.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PriceChart(coin: Coin) {
    if (coin.sparkline.size < 2) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row {
            Text("Últimos 7 días", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("Máx ${formatPrice(coin.sparkline.max(), coin.currency)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        Sparkline(
            points = coin.sparkline,
            color = trendColor(coin.change7d),
            fill = true,
            strokeWidth = 5f,
            modifier = Modifier.fillMaxWidth().height(180.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Mín ${formatPrice(coin.sparkline.min(), coin.currency)}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End)
        )
    }
}
