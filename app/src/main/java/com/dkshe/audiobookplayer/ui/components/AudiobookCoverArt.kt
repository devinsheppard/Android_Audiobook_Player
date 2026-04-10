package com.dkshe.audiobookplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dkshe.audiobookplayer.R

@Composable
fun AudiobookCoverArt(
    coverArtPath: String?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
) {
    if (coverArtPath.isNullOrBlank()) {
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Icon(
                imageVector = Icons.Rounded.MenuBook,
                contentDescription = contentDescription ?: stringResource(R.string.cover_art_placeholder),
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.Center),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        AsyncImage(
            model = coverArtPath,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.aspectRatio(1f),
        )
    }
}
