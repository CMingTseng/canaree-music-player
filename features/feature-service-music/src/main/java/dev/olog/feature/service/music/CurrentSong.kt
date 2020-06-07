package dev.olog.feature.service.music

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import dev.olog.core.dagger.FeatureScope
import dev.olog.domain.MediaIdCategory
import dev.olog.domain.entity.LastMetadata
import dev.olog.domain.entity.favorite.FavoriteItemState
import dev.olog.domain.entity.favorite.FavoriteState
import dev.olog.domain.entity.favorite.FavoriteTrackType
import dev.olog.domain.interactor.InsertHistorySongUseCase
import dev.olog.domain.interactor.favorite.IsFavoriteSongUseCase
import dev.olog.domain.interactor.favorite.UpdateFavoriteStateUseCase
import dev.olog.domain.interactor.lastplayed.InsertLastPlayedAlbumUseCase
import dev.olog.domain.interactor.lastplayed.InsertLastPlayedArtistUseCase
import dev.olog.domain.interactor.mostplayed.InsertMostPlayedUseCase
import dev.olog.domain.prefs.MusicPreferencesGateway
import dev.olog.domain.schedulers.Schedulers
import dev.olog.core.dagger.ServiceLifecycle
import dev.olog.feature.service.music.interfaces.IPlayerLifecycle
import dev.olog.feature.service.music.model.MediaEntity
import dev.olog.feature.service.music.model.MetadataEntity
import dev.olog.shared.coroutines.autoDisposeJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@FeatureScope
internal class CurrentSong @Inject constructor(
    @ServiceLifecycle private val lifecycle: Lifecycle,
    insertMostPlayedUseCase: InsertMostPlayedUseCase,
    insertHistorySongUseCase: InsertHistorySongUseCase,
    private val musicPreferencesUseCase: MusicPreferencesGateway,
    private val isFavoriteSongUseCase: IsFavoriteSongUseCase,
    private val updateFavoriteStateUseCase: UpdateFavoriteStateUseCase,
    insertLastPlayedAlbumUseCase: InsertLastPlayedAlbumUseCase,
    insertLastPlayedArtistUseCase: InsertLastPlayedArtistUseCase,
    playerLifecycle: IPlayerLifecycle,
    private val schedulers: Schedulers

) : DefaultLifecycleObserver,
    IPlayerLifecycle.Listener {

    companion object {
        @JvmStatic
        private val TAG = "SM:${CurrentSong::class.java.simpleName}"
    }

    private var isFavoriteJob by autoDisposeJob()

    private val channel = Channel<MediaEntity>(Channel.UNLIMITED)

    init {
        lifecycle.addObserver(this)
        playerLifecycle.addListener(this)

        lifecycle.coroutineScope.launch(schedulers.cpu) {
            for (entity in channel) {
                Timber.v("$TAG on new item ${entity.title}")
                val mediaId = entity.mediaId

                when (mediaId.category) {
                    MediaIdCategory.ARTISTS,
                    MediaIdCategory.PODCASTS_AUTHORS -> {
                        Timber.v("$TAG insert last played artist ${entity.title}")
                        insertLastPlayedArtistUseCase(entity.mediaId.parentId)
                    }
                    MediaIdCategory.ALBUMS -> {
                        Timber.v("$TAG insert last played album ${entity.title}")
                        insertLastPlayedAlbumUseCase(entity.mediaId.parentId)
                    }
                    else -> {}
                }

                Timber.v("$TAG insert most played ${entity.title}")
                insertMostPlayedUseCase(entity.mediaId)

                Timber.v("$TAG insert to history ${entity.title}")
                insertHistorySongUseCase(
                    InsertHistorySongUseCase.Input(entity.id, entity.isPodcast)
                )
            }
        }

    }

    override fun onDestroy(owner: LifecycleOwner) {
        channel.close()
    }

    override fun onPrepare(metadata: MetadataEntity) {
        updateFavorite(metadata.entity)
        saveLastMetadata(metadata.entity)
    }

    override fun onMetadataChanged(metadata: MetadataEntity) {
        channel.offer(metadata.entity)
        updateFavorite(metadata.entity)
        saveLastMetadata(metadata.entity)
    }

    private fun updateFavorite(mediaEntity: MediaEntity) {
        Timber.v("$TAG updateFavorite ${mediaEntity.title}")

        isFavoriteJob = lifecycle.coroutineScope.launch(schedulers.cpu) {
            val type = if (mediaEntity.isPodcast) FavoriteTrackType.PODCAST else FavoriteTrackType.TRACK
            val isFavorite =
                isFavoriteSongUseCase(mediaEntity.id, type)
            val isFavoriteEnum =
                if (isFavorite) FavoriteState.FAVORITE else FavoriteState.NOT_FAVORITE
            updateFavoriteStateUseCase(FavoriteItemState(mediaEntity.id, isFavoriteEnum, type))
        }
    }

    private fun saveLastMetadata(entity: MediaEntity) {
        Timber.v("$TAG saveLastMetadata ${entity.title}")
        lifecycle.coroutineScope.launch(schedulers.cpu) {
            musicPreferencesUseCase.setLastMetadata(
                LastMetadata(entity.title, entity.artist, entity.id)
            )
        }
    }

}