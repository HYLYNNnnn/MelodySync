package com.laioffer.spotify.ui.favorite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.laioffer.spotify.R
import com.laioffer.spotify.datamodel.Album

@Composable
fun FavoriteScreen(viewModel: FavoriteViewModel, onTap: (Album) -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> Text(stringResource(R.string.screen_loading), modifier = Modifier.padding(16.dp))
        uiState.error != null -> Column(Modifier.padding(16.dp)) {
            Text(uiState.error.orEmpty(), color = MaterialTheme.colors.error)
            Button(onClick = viewModel::retry) { Text(stringResource(R.string.retry)) }
        }
        else -> FavoriteScreenContent(uiState.albums, onTap)
    }

}


@Composable
private fun FavoriteScreenContent(albums: List<Album>, onTap: (Album) -> Unit) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text(
                stringResource(id = R.string.menu_favorite),
                style = MaterialTheme.typography.h4,
                color = Color.White
            )
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (albums.isEmpty()) item { Text(stringResource(R.string.empty_favorites), color = Color.LightGray) }
        items(albums, key = { it.id }) { album ->
            FavoriteAlbumRow(
                album = album,
                onTap = onTap
            )
        }

    }
}

@Composable
private fun FavoriteAlbumRow(album: Album, onTap: (Album) -> Unit) {
    Row(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .clickable {
                onTap(album)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = album.cover,
                placeholder = painterResource(R.drawable.album_placeholder),
                error = painterResource(R.drawable.album_placeholder),
            contentDescription = null,
            modifier = Modifier
                .width(60.dp)
                .aspectRatio(1.0f)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.FillBounds
        )
        Column(
            modifier = Modifier
                .weight(1.0f)
                .padding(start = 8.dp)
        ) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.body2,
                color = Color.White,
            )

            Text(
                text = stringResource(id = R.string.album_info, album.artists, album.year),
                style = MaterialTheme.typography.caption,
                color = Color.Gray,
            )
        }
    }
}