package com.laioffer.spotify.ui.playlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.laioffer.spotify.R
import com.laioffer.spotify.datamodel.Album
import com.laioffer.spotify.datamodel.Song
import com.laioffer.spotify.player.PlayerViewModel
import com.laioffer.spotify.ui.artwork.artworkModel

@Composable
fun PlaylistScreen(playlistViewModel: PlaylistViewModel, playerViewModel: PlayerViewModel) {
    val state by playlistViewModel.uiState.collectAsStateWithLifecycle()
    val playback by playerViewModel.uiState.collectAsStateWithLifecycle()
    // One scroll container keeps tracks reachable on small screens and in landscape.
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            AlbumCover(state.album, state.isFavorite, state.isSavingFavorite, playlistViewModel::toggleFavorite)
            Text(state.album.name, style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp))
            Text(stringResource(R.string.album_info, state.album.artists, state.album.year), color = Color.LightGray)
            Text(state.album.description, style = MaterialTheme.typography.body2, color = Color.LightGray,
                modifier = Modifier.padding(vertical = 12.dp))
            state.favoriteError?.let { RetryMessage(it, playlistViewModel::retry) }
            when {
                state.isLoading -> Text(stringResource(R.string.screen_loading), modifier = Modifier.padding(vertical = 16.dp))
                state.error != null -> RetryMessage(state.error.orEmpty(), playlistViewModel::retry)
                state.playlist.isEmpty() -> Text(stringResource(R.string.empty_playlist), modifier = Modifier.padding(vertical = 16.dp))
            }
        }
        items(state.playlist) { song ->
            SongRow(song, playback.song == song && playback.isPlaying) {
                if (playback.song == song) playerViewModel.togglePlayback()
                else { playerViewModel.load(song, state.album); playerViewModel.play() }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun RetryMessage(message: String, onRetry: () -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(message, color = MaterialTheme.colors.error)
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun SongRow(song: Song, isPlaying: Boolean, onTap: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onTap)
        .semantics { contentDescription = "Play ${song.name}" }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(song.name, color = if (isPlaying) Color.Green else Color.White)
            Text(song.lyric, style = MaterialTheme.typography.caption, color = Color.LightGray)
        }
        Text(song.length, style = MaterialTheme.typography.caption, color = Color.LightGray,
            modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun AlbumCover(album: Album, favorite: Boolean, saving: Boolean, onFavorite: (Boolean) -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        IconButton(onClick = { onFavorite(!favorite) }, enabled = !saving, modifier = Modifier.align(Alignment.TopEnd)) {
            Icon(painterResource(if (favorite) R.drawable.ic_favorite_24 else R.drawable.ic_unfavorite_24),
                contentDescription = stringResource(if (favorite) R.string.unfavorite_album else R.string.favorite_album),
                tint = if (favorite) Color.Green else Color.LightGray)
        }
        Box(Modifier.align(Alignment.Center).widthIn(max = 240.dp).fillMaxWidth(0.7f).aspectRatio(1f)) {
            Image(painterResource(R.drawable.vinyl_background), contentDescription = null, modifier = Modifier.fillMaxSize())
            AsyncImage(model = album.artworkModel(),
                placeholder = painterResource(R.drawable.album_placeholder),
                error = painterResource(R.drawable.album_placeholder), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.6f).aspectRatio(1f).clip(CircleShape))
        }
    }
}
