package dev.olog.feature.detail.mapper

import android.content.res.Resources
import dev.olog.domain.entity.spotify.SpotifyAlbum
import dev.olog.domain.entity.spotify.SpotifyTrack
import dev.olog.feature.detail.R
import dev.olog.feature.presentation.base.model.DisplayableAlbum
import dev.olog.feature.presentation.base.model.DisplayableTrack
import dev.olog.feature.presentation.base.model.toPresentation

internal fun SpotifyAlbum.toDetailDisplayableItem(resources: Resources): DisplayableAlbum {
    return DisplayableAlbum(
        type = R.layout.item_detail_album_spotify,
        mediaId = mediaId.toPresentation(),
        title = title,
        subtitle = resources.getQuantityString(
            R.plurals.common_plurals_song,
            this.songs,
            this.songs
        ).toLowerCase()
    )
}

internal fun SpotifyTrack.toDetailDisplayableItem(): DisplayableTrack {
    return DisplayableTrack(
        type = R.layout.item_detail_song_with_track_spotify,
        mediaId = mediaId.toPresentation(),
        title = name,
        artist = artist,
        album = album,
        idInPlaylist = trackNumber,
        dataModified = -1,
        duration = this.duration,
        isExplicit = this.isExplicit
    )
}