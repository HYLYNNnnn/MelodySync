package com.laioffer.spotify

import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.laioffer.spotify.player.PlayerViewModel
import com.laioffer.spotify.ui.favorite.FavoriteFragment
import com.laioffer.spotify.ui.home.HomeFragment
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Launches the production Hilt application; no mock backend or test-only activity is used. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = MainApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class UiStartupTest {
    @Test fun launchConnectsHomeNavigationBottomMenuAndPlayerHost() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.supportFragmentManager.executePendingTransactions()
                assertTrue(activity.application is MainApplication)
                val navHost = activity.supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                navHost.childFragmentManager.executePendingTransactions()
                assertEquals(R.id.homeFragment, navHost.navController.currentDestination?.id)
                assertTrue(navHost.childFragmentManager.primaryNavigationFragment is HomeFragment)
                val bottomNavigation = activity.findViewById<BottomNavigationView>(R.id.nav_view)
                assertEquals(2, bottomNavigation.menu.size())
                assertEquals(R.id.homeFragment, bottomNavigation.selectedItemId)
                assertNotNull(bottomNavigation.menu.findItem(R.id.favoriteFragment))
                assertTrue(activity.findViewById<ComposeView>(R.id.mini_player).isAttachedToWindow)
            }
        }
    }

    @Test fun favoritesNavigationAndRecreationKeepThePlayerOwnerAndDestination() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var originalPlayer: PlayerViewModel? = null
            scenario.onActivity { activity ->
                originalPlayer = ViewModelProvider(activity)[PlayerViewModel::class.java]
                activity.findViewById<BottomNavigationView>(R.id.nav_view)
                    .selectedItemId = R.id.favoriteFragment
                activity.supportFragmentManager.executePendingTransactions()
                val navHost = activity.supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                navHost.childFragmentManager.executePendingTransactions()
                assertEquals(R.id.favoriteFragment, navHost.navController.currentDestination?.id)
                assertTrue(navHost.childFragmentManager.primaryNavigationFragment is FavoriteFragment)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                activity.supportFragmentManager.executePendingTransactions()
                val navHost = activity.supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                assertEquals(R.id.favoriteFragment, navHost.navController.currentDestination?.id)
                assertEquals(R.id.favoriteFragment, activity.findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId)
                assertSame(originalPlayer, ViewModelProvider(activity)[PlayerViewModel::class.java])
                assertNotNull(activity.findViewById<ComposeView>(R.id.mini_player))
            }
        }
    }
}
