package com.laioffer.spotify.ui.favorite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.laioffer.spotify.ui.theme.SpotifyTheme
import com.laioffer.spotify.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FavoriteFragment : Fragment() {
    private val viewModel: FavoriteViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SpotifyTheme(darkTheme = true) {
                    FavoriteScreen(viewModel, onTap = { album ->
                        val navController = findNavController()
                        if (navController.currentDestination?.id == R.id.favoriteFragment) {
                            navController.navigate(FavoriteFragmentDirections.actionFavoriteFragmentToPlaylistFragment(album))
                        }
                    })
                }
            }
        }
}
