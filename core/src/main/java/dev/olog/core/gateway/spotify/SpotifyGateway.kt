package dev.olog.core.gateway.spotify

import dev.olog.core.MediaId
import dev.olog.core.entity.spotify.SpotifyAlbum
import dev.olog.core.entity.spotify.SpotifyAlbumType
import dev.olog.core.entity.spotify.SpotifyTrack

interface SpotifyGateway {

    suspend fun getArtistAlbums(artistMediaId: MediaId.Category, type: SpotifyAlbumType): List<SpotifyAlbum>

    suspend fun getArtistTopTracks(artistMediaId: MediaId.Category): List<SpotifyTrack>

    fun getImage(spotifyUri: String): String?

}