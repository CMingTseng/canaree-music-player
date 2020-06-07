package dev.olog.feature.service.music.queue

import android.net.Uri
import android.os.Bundle
import dev.olog.domain.MediaId
import dev.olog.domain.entity.PureUri
import dev.olog.domain.entity.track.Song
import dev.olog.domain.gateway.PlayingQueueGateway
import dev.olog.domain.gateway.spotify.SpotifyGateway
import dev.olog.domain.gateway.track.GenreGateway
import dev.olog.domain.gateway.track.TrackGateway
import dev.olog.domain.interactor.mostplayed.ObserveMostPlayedSongsUseCase
import dev.olog.domain.interactor.ObserveRecentlyAddedUseCase
import dev.olog.domain.interactor.PodcastPositionUseCase
import dev.olog.domain.interactor.songlist.GetSongListByParamUseCase
import dev.olog.domain.prefs.MusicPreferencesGateway
import dev.olog.domain.schedulers.Schedulers
import dev.olog.feature.service.music.interfaces.IQueue
import dev.olog.feature.service.music.model.*
import dev.olog.feature.service.music.state.MusicServiceShuffleMode
import dev.olog.feature.service.music.voice.VoiceSearch
import dev.olog.feature.service.music.voice.VoiceSearchParams
import dev.olog.shared.android.utils.assertBackgroundThread
import dev.olog.shared.android.utils.assertMainThread
import dev.olog.shared.clamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class QueueManager @Inject constructor(
    private val queueImpl: QueueImpl,
    private val playingQueueGateway: PlayingQueueGateway,
    private val musicPreferencesUseCase: MusicPreferencesGateway,
    private val shuffleMode: MusicServiceShuffleMode,
    private val getSongListByParamUseCase: GetSongListByParamUseCase,
    private val getMostPlayedSongsUseCase: ObserveMostPlayedSongsUseCase,
    private val getRecentlyAddedUseCase: ObserveRecentlyAddedUseCase,
    private val trackGateway: TrackGateway,
    private val genreGateway: GenreGateway,
    private val enhancedShuffle: EnhancedShuffle,
    private val podcastPosition: PodcastPositionUseCase,
    private val schedulers: Schedulers,
    private val spotifyGateway: SpotifyGateway

) : IQueue {

    override fun prepare(): PlayerMediaEntity? {
        assertMainThread()

        val playingQueue = playingQueueGateway.getAll().map { it.toMediaEntity() }

        val lastPlayedId = musicPreferencesUseCase.getLastIdInPlaylist()
        val currentPosition = clamp(
            playingQueue.indexOfFirst { it.idInPlaylist == lastPlayedId },
            0,
            playingQueue.lastIndex
        )

        val result = playingQueue.getOrNull(currentPosition) ?: return null

        queueImpl.updateState(
            playingQueue, currentPosition,
            updateImmediate = true, persist = false
        )



        return result.toPlayerMediaEntity(
            queueImpl.computePositionInQueue(playingQueue, currentPosition),
            getLastSessionBookmark(result)
        )
    }

    override fun isEmpty(): Boolean {
        return queueImpl.isEmpty()
    }

    override suspend fun handlePlayFromMediaId(mediaId: MediaId, filter: String?): PlayerMediaEntity? {
        assertBackgroundThread()

        val songId = when (mediaId) {
            is MediaId.Track -> mediaId.id.toLong()
            is MediaId.Category -> -1
        }
        val parentId = when (mediaId) {
            is MediaId.Track -> mediaId.parentId
            is MediaId.Category -> mediaId
        }

        val songList = getSongListByParamUseCase(mediaId).asSequence()
            .filterSongList(filter)
            .mapIndexed { index, song -> song.toMediaEntity(index, parentId) }
            .toList()

        shuffleMode.setEnabled(false)

        val currentIndex = getCurrentSongIndexWhenPlayingNewQueue(songList, songId)
        val result = songList.getOrNull(currentIndex) ?: return null

        queueImpl.updateState(
            songList, currentIndex,
            updateImmediate = false,
            persist = true
        )


        return result.toPlayerMediaEntity(
            queueImpl.computePositionInQueue(songList, currentIndex),
            getPodcastBookmarkOrDefault(result)
        )
    }

    override suspend fun handlePlayRecentlyAdded(mediaId: MediaId.Track): PlayerMediaEntity? {
        assertBackgroundThread()

        val songId = mediaId.id.toLong()

        val songList = getRecentlyAddedUseCase(mediaId.parentId).first()
            .mapIndexed { index, song -> song.toMediaEntity(index, mediaId.parentId) }

        shuffleMode.setEnabled(false)

        val currentIndex = getCurrentSongIndexWhenPlayingNewQueue(songList, songId)
        val result = songList.getOrNull(currentIndex) ?: return null

        queueImpl.updateState(
            songList = songList,
            index = currentIndex,
            updateImmediate = false,
            persist = true
        )

        return result.toPlayerMediaEntity(
            queueImpl.computePositionInQueue(songList, currentIndex),
            getPodcastBookmarkOrDefault(result)
        )
    }

    override suspend fun handlePlayMostPlayed(mediaId: MediaId.Track): PlayerMediaEntity? {
        assertBackgroundThread()

        val songId = mediaId.id.toLong()

        val songList = getMostPlayedSongsUseCase(mediaId.parentId).first()
            .mapIndexed { index, song -> song.toMediaEntity(index, mediaId.parentId) }

        shuffleMode.setEnabled(false)

        val currentIndex = getCurrentSongIndexWhenPlayingNewQueue(songList, songId)
        val result = songList.getOrNull(currentIndex) ?: return null

        queueImpl.updateState(
            songList = songList,
            index = currentIndex,
            updateImmediate = false,
            persist = true
        )

        return result.toPlayerMediaEntity(
            queueImpl.computePositionInQueue(songList, currentIndex),
            getPodcastBookmarkOrDefault(result)
        )
    }

    override suspend fun handlePlayShuffle(mediaId: MediaId.Category, filter: String?): PlayerMediaEntity? {
        assertBackgroundThread()

        var songList = getSongListByParamUseCase(mediaId).asSequence()
            .filterSongList(filter)
            .mapIndexed { index, song -> song.toMediaEntity(index, mediaId) }
            .toList()

        shuffleMode.setEnabled(true)
        songList = shuffle(songList)

        val currentIndex = 0
        val result = songList.getOrNull(currentIndex) ?: return null

        queueImpl.updateState(
            songList, currentIndex,
            updateImmediate = false,
            persist = true
        )

        return result.toPlayerMediaEntity(
            queueImpl.computePositionInQueue(songList, currentIndex),
            getPodcastBookmarkOrDefault(result)
        )
    }

    override suspend fun handlePlayFromUri(uri: Uri): PlayerMediaEntity? {
        assertBackgroundThread()

        val pureUri = PureUri(uri.scheme!!, uri.scheme!!, uri.fragment)
        val song = trackGateway.getByUri(pureUri) ?: return null
        val mediaEntity = song.toMediaEntity(0, song.parentMediaId)
        val songList = listOf(mediaEntity)

        val currentIndex = 0
        val result = songList.getOrNull(currentIndex) ?: return null

        queueImpl.updateState(
            songList, currentIndex,
            updateImmediate = false,
            persist = true
        )

        return result.toPlayerMediaEntity(
            queueImpl.computePositionInQueue(songList, currentIndex),
            getPodcastBookmarkOrDefault(result)
        )
    }

    override suspend fun handlePlaySpotifyPreview(mediaId: MediaId.Track): PlayerMediaEntity? {
        val uri = mediaId.categoryId
        val trackId = uri.drop(uri.lastIndexOf(":") + 1)
        val track = spotifyGateway.getTrack(trackId) ?: return null

        val mediaEntity = MediaEntity(
            track.id.hashCode().toLong(), 0, mediaId, -1, -1,
            track.name, track.artist, track.artist, track.album,
            TimeUnit.SECONDS.toMillis(30), -1, "",
            track.discNumber, track.trackNumber,
            false, Uri.parse(mediaId.id)
        )
        val songList = listOf(mediaEntity)

        val currentIndex = 0
        val result = songList.getOrNull(currentIndex) ?: return null

        queueImpl.updateState(
            songList, currentIndex,
            updateImmediate = false,
            persist = true
        )
        return result.toPlayerMediaEntity(
            queueImpl.computePositionInQueue(songList, currentIndex),
            getPodcastBookmarkOrDefault(result)
        )
    }

    override suspend fun handlePlayFromGoogleSearch(
        query: String,
        extras: Bundle
    ): PlayerMediaEntity? {
        Timber.d("VoiceSearch: Creating playing queue for musics from search: $query, params=$extras")

        val params = VoiceSearchParams(query, extras)

        val mediaId = MediaId.SONGS_CATEGORY

        var forceShuffle = false

        val songList: List<MediaEntity> = when {
            params.isUnstructured -> VoiceSearch.search(
                getSongListByParamUseCase(mediaId),
                query
            )
            params.isAlbumFocus -> VoiceSearch.filterByAlbum(
                getSongListByParamUseCase(mediaId),
                params.album
            )
            params.isArtistFocus -> VoiceSearch.filterByArtist(
                getSongListByParamUseCase(mediaId),
                params.artist
            )
            params.isSongFocus -> VoiceSearch.filterByTrack(
                getSongListByParamUseCase(mediaId),
                params.song
            )
            params.isGenreFocus -> VoiceSearch.filterByGenre(genreGateway, params.genre)
            else -> {
                forceShuffle = true
                VoiceSearch.noFilter(getSongListByParamUseCase(mediaId).shuffled())
            }
        }

        shuffleMode.setEnabled(forceShuffle)

        val currentIndex = 0
        val result = songList.getOrNull(currentIndex) ?: return null

        queueImpl.updateState(
            songList, currentIndex,
            updateImmediate = false,
            persist = true
        )

        return result.toPlayerMediaEntity(
            queueImpl.computePositionInQueue(songList, currentIndex),
            getPodcastBookmarkOrDefault(result)
        )
    }

    private fun shuffle(songList: List<MediaEntity>): List<MediaEntity> {
        return enhancedShuffle.shuffle(songList)
    }

    private fun getCurrentSongIndexWhenPlayingNewQueue(
        songList: List<MediaEntity>,
        songId: Long
    ): Int {
        if (shuffleMode.isEnabled() || songId == -1L) {
            return 0
        } else {
            return clamp(
                songList.indexOfFirst { it.id == songId },
                0,
                songList.lastIndex
            )
        }
    }

    override suspend fun handleSkipToQueueItem(idInPlaylist: Long): PlayerMediaEntity? {
        assertMainThread()

        val mediaEntity = queueImpl.getSongById(idInPlaylist.toInt()) ?: return null
        return mediaEntity.toPlayerMediaEntity(
            queueImpl.currentPositionInQueue(),
            getPodcastBookmarkOrDefault(mediaEntity)
        )
    }

    override suspend fun handleSkipToNext(trackEnded: Boolean): PlayerMediaEntity? {
        val mediaEntity = queueImpl.getNextSong(trackEnded) ?: return null
        return mediaEntity.toPlayerMediaEntity(
            queueImpl.currentPositionInQueue(),
            getPodcastBookmarkOrDefault(mediaEntity)
        )
    }

    override suspend fun handleSkipToPrevious(playerBookmark: Long): PlayerMediaEntity? {
        val mediaEntity = queueImpl.getPreviousSong(playerBookmark)
        val bookmark = getPodcastBookmarkOrDefault(mediaEntity)
        return mediaEntity?.toPlayerMediaEntity(
            queueImpl.currentPositionInQueue(),
            bookmark
        )
    }

    override suspend fun getPlayingSong(): PlayerMediaEntity? {
        val mediaEntity = queueImpl.getCurrentSong() ?: return null
        return mediaEntity.toPlayerMediaEntity(
            queueImpl.currentPositionInQueue(),
            getPodcastBookmarkOrDefault(mediaEntity)
        )
    }


    private fun getLastSessionBookmark(mediaEntity: MediaEntity): Long  {
        if (mediaEntity.isPodcast) {
            val bookmark = podcastPosition.get(mediaEntity.id, mediaEntity.duration)
            return clamp(bookmark, 0L, mediaEntity.duration)
        } else {
            val bookmark = musicPreferencesUseCase.getBookmark().toInt()
            return clamp(
                bookmark.toLong(),
                0L,
                mediaEntity.duration
            )
        }
    }

    private suspend fun getPodcastBookmarkOrDefault(
        mediaEntity: MediaEntity?,
        default: Long = 0L
    ): Long = withContext(schedulers.cpu) {
        if (mediaEntity?.isPodcast == true) {
            val bookmark = podcastPosition.get(mediaEntity.id, mediaEntity.duration)
            clamp(bookmark, 0L, mediaEntity.duration)
        } else {
            default
        }
    }

    override fun handleSwap(from: Int, to: Int) {
        queueImpl.handleSwap(from, to)
    }

    override fun handleSwapRelative(from: Int, to: Int) {
        queueImpl.handleSwapRelative(from, to)
    }

    override fun handleMoveRelative(position: Int) {
        queueImpl.handleMoveRelative(position)
    }

    override fun handleRemove(position: Int) {
        queueImpl.handleRemove(position)
    }

    override fun handleRemoveRelative(position: Int) {
        queueImpl.handleRemoveRelative(position)
    }

    override fun sort() {
        queueImpl.sort()
    }

    override fun shuffle() {
        queueImpl.shuffle()
    }

    override fun getCurrentPositionInQueue(): PositionInQueue {
        return queueImpl.currentPositionInQueue()
    }

    override fun onRepeatModeChanged() {
        queueImpl.onRepeatModeChanged()
    }

    override suspend fun playLater(
        songIds: List<Long>
    ): PositionInQueue {
        val currentPositionInQueue = getCurrentPositionInQueue()
        queueImpl.playLater(songIds)
        return when (currentPositionInQueue) {
            PositionInQueue.FIRST_AND_LAST -> PositionInQueue.FIRST
            PositionInQueue.LAST -> PositionInQueue.IN_MIDDLE
            else -> currentPositionInQueue
        }
    }

    override suspend fun playNext(
        songIds: List<Long>
    ): PositionInQueue {
        val currentPositionInQueue = getCurrentPositionInQueue()
        queueImpl.playNext(songIds)
        return when (currentPositionInQueue) {
            PositionInQueue.FIRST_AND_LAST -> PositionInQueue.FIRST
            PositionInQueue.LAST -> PositionInQueue.IN_MIDDLE
            else -> currentPositionInQueue
        }
    }

    override fun updatePodcastPosition(position: Long) {
        val mediaEntity = queueImpl.getCurrentSong()
        if (mediaEntity?.isPodcast == true) {
            podcastPosition.set(mediaEntity.id, position)
        }
    }

    private fun Sequence<Song>.filterSongList(filter: String?): Sequence<Song> {
        return this.filter {
            if (filter.isNullOrBlank()) {
                true
            } else {
                it.title.contains(filter, true) ||
                        it.artist.contains(filter, true) ||
                        it.album.contains(filter, true)
            }
        }
    }

}