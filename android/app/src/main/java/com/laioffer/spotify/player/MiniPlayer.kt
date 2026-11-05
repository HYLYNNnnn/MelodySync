package com.laioffer.spotify.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.laioffer.spotify.R

@Composable
fun MiniPlayer(playerViewModel: PlayerViewModel, modifier: Modifier = Modifier) {
    val state by playerViewModel.uiState.collectAsState()
    val song = state.song ?: return
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colors.surface, elevation = 6.dp) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    state.album?.let { Text(it.artists, style = MaterialTheme.typography.caption, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                IconButton(onClick = playerViewModel::togglePlayback, enabled = state.error == null) {
                    Icon(
                        painter = painterResource(if (state.playWhenReady) R.drawable.ic_pause_24 else R.drawable.ic_play_arrow_24),
                        contentDescription = if (state.playWhenReady) "Pause music" else "Play music"
                    )
                }
                TextButton(onClick = playerViewModel::stop) { Text("Close") }
            }
            state.error?.let { error ->
                Text(error, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = playerViewModel::retry) { Text("Retry playback") }
                    Spacer(Modifier.width(8.dp))
                }
            }
        }
    }
}
