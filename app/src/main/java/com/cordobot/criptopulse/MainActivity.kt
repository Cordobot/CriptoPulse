package com.cordobot.criptopulse

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cordobot.criptopulse.presentation.detail.CoinDetailScreen
import com.cordobot.criptopulse.presentation.market.MarketScreen
import com.cordobot.criptopulse.presentation.theme.CriptoPulseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            CriptoPulseTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    NavHost(navController, startDestination = "market") {
                        composable("market") {
                            MarketScreen(onOpenCoin = { id -> navController.navigate("coin/$id") })
                        }
                        composable(
                            "coin/{id}",
                            enterTransition = { slideInHorizontally { it / 3 } + fadeIn() },
                            popExitTransition = { slideOutHorizontally { it / 3 } + fadeOut() }
                        ) {
                            CoinDetailScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
