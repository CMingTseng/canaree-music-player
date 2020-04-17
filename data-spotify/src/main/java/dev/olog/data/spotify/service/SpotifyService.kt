package dev.olog.data.spotify.service

import dev.olog.data.spotify.dto.RemoteSpotifyTrack
import dev.olog.data.spotify.dto.RemoteSpotifyTrackAudioFeature
import dev.olog.data.spotify.dto.complex.*
import dev.olog.lib.network.retrofit.IoResult
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// https://developer.spotify.com/documentation/web-api/reference/search/search/
// TODO what is it include_external=audio??
// TODO marker, what market to use? US may not be the best
internal interface SpotifyService {

    @GET("search?offset=0&limit=10&type=artist&market=US")
    suspend fun searchArtist(
        @Query("q") query: String
    ): IoResult<RemoteSpotifySearchArtist>

    @GET("search?offset=0&limit=10&type=album&market=US")
    suspend fun searchAlbum(
        @Query("q") query: String
    ): IoResult<RemoteSpotifySearchAlbums>

    @GET("search?offset=0&limit=1&type=track&market=US")
    suspend fun searchTrack(
        @Query("q") query: String
    ): IoResult<RemoteSpotifySearchTracks>

    @GET("artists/{id}/albums?&country=US&limit=50")
    suspend fun getArtistAlbums(
        @Path("id") artistId: String,
        @Query("include_groups") type: String
    ): IoResult<RemoteSpotifyArtistAlbum>

    @GET("artists/{id}/top-tracks")
    suspend fun getArtistTopTracks(
        @Path("id") artistId: String
    ): IoResult<RemoteSpotifyArtistTopTracks>

    @GET("albums/{id}/tracks?limit=50&market=US")
    suspend fun getAlbumTracks(
        @Path("id") albumId: String
    ): IoResult<RemoteSpotifyAlbumTracks>

    @GET("tracks/{id}")
    suspend fun getTrack(
        @Path("id") trackId: String
    ): IoResult<RemoteSpotifyTrack>

    @GET("audio-features/{id}")
    suspend fun getTrackAudioFeature(
        @Path("id") trackId: String
    ): IoResult<RemoteSpotifyTrackAudioFeature>

}



