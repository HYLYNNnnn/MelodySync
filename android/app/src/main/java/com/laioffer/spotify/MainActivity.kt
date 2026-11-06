package com.laioffer.spotify

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.laioffer.spotify.player.MiniPlayer
import com.laioffer.spotify.player.PlayerViewModel
import com.laioffer.spotify.ui.theme.SpotifyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        findViewById<BottomNavigationView>(R.id.nav_view).apply {
            setupWithNavController(navHost.navController)
            // Album details keep their parent tab selected; tapping it returns to that tab.
            setOnItemReselectedListener { item ->
                navHost.navController.popBackStack(item.itemId, false)
            }
        }
        findViewById<ComposeView>(R.id.mini_player).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { SpotifyTheme(darkTheme = true) { MiniPlayer(playerViewModel) } }
        }
    }

    override fun onStop() {
        // This app supports foreground playback; rotating the screen keeps playback alive.
        if (!isChangingConfigurations) playerViewModel.pause()
        super.onStop()
    }
}
