package com.laioffer.spotify.ui.artwork

import com.laioffer.spotify.R
import com.laioffer.spotify.datamodel.Album

/** Catalog artwork is bundled, including for favorites saved with older remote URLs. */
fun Album.artworkModel(): Any = when (id) {
    1 -> R.drawable.album_hexagonal
    2 -> R.drawable.album_still_fantasy
    3 -> R.drawable.album_dangerous
    4 -> R.drawable.album_kite
    5 -> R.drawable.album_no_89757
    6 -> R.drawable.album_nxde_remix
    7 -> R.drawable.album_blackpink_in_your_area
    else -> if (cover.isBlank()) R.drawable.album_placeholder else cover
}
