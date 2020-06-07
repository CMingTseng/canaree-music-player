package dev.olog.feature.service.music.notification

import android.app.Notification
import android.app.Service
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import dev.olog.core.dagger.FeatureScope
import dev.olog.domain.entity.favorite.FavoriteState
import dev.olog.domain.interactor.favorite.ObserveFavoriteAnimationUseCase
import dev.olog.domain.schedulers.Schedulers
import dev.olog.core.dagger.ServiceLifecycle
import dev.olog.feature.service.music.interfaces.INotification
import dev.olog.feature.service.music.interfaces.IPlayerLifecycle
import dev.olog.feature.service.music.model.Event
import dev.olog.feature.service.music.model.MediaEntity
import dev.olog.feature.service.music.model.MetadataEntity
import dev.olog.feature.service.music.model.MusicNotificationState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

@FeatureScope
internal class MusicNotificationManager @Inject constructor(
    @ServiceLifecycle private val lifecycle: Lifecycle,
    private val service: Service,
    private val notificationImpl: INotification,
    observeFavoriteUseCase: ObserveFavoriteAnimationUseCase,
    playerLifecycle: IPlayerLifecycle,
    schedulers: Schedulers

) : DefaultLifecycleObserver {

    companion object {
        @JvmStatic
        private val TAG = "SM:${MusicNotificationManager::class.java.simpleName}"
    }

    private var isForeground: Boolean = false

    private val publisher = Channel<Event>(Channel.UNLIMITED)
    private val currentState = MusicNotificationState()

    private val playerListener = object : IPlayerLifecycle.Listener {
        override fun onPrepare(metadata: MetadataEntity) {
            onNextMetadata(metadata.entity)
        }

        override fun onMetadataChanged(metadata: MetadataEntity) {
            onNextMetadata(metadata.entity)
        }

        override fun onStateChanged(state: PlaybackStateCompat) {
            onNextState(state)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stopForeground()
        publisher.close()
    }

    private fun onNextMetadata(metadata: MediaEntity) {
        Timber.v("$TAG on next metadata=${metadata.title}")
        publisher.offer(Event.Metadata(metadata))
    }

    private fun onNextState(playbackState: PlaybackStateCompat) {
        Timber.v("$TAG on next state")
        publisher.offer(Event.State(playbackState))
    }

    private fun onNextFavorite(isFavorite: Boolean) {
        Timber.v("$TAG on next favorite $isFavorite")
        publisher.offer(Event.Favorite(isFavorite))
    }

    init {
        lifecycle.addObserver(this)
        playerLifecycle.addListener(playerListener)

        publisher.consumeAsFlow()
            .filter { event ->
                when (event) {
                    is Event.Metadata -> currentState.isDifferentMetadata(event.entity)
                    is Event.State -> currentState.isDifferentState(event.state)
                    is Event.Favorite -> currentState.isDifferentFavorite(event.favorite)
                }
            }
            .onEach { consumeEvent(it) }
            .flowOn(schedulers.cpu)
            .launchIn(lifecycle.coroutineScope)

        observeFavoriteUseCase()
            .map { it == FavoriteState.FAVORITE }
            .onEach { onNextFavorite(it) }
            .flowOn(schedulers.cpu)
            .launchIn(lifecycle.coroutineScope)
    }

    private suspend fun consumeEvent(event: Event){
        Timber.v("$TAG on next event $event")

        when (event){
            is Event.Metadata -> {
                if (currentState.updateMetadata(event.entity)) {
                    publishNotification(currentState.copy())
                }
            }
            is Event.State -> {
                if (currentState.updateState(event.state)){
                    publishNotification(currentState.copy())
                }
            }
            is Event.Favorite -> {
                if (currentState.updateFavorite(event.favorite)){
                    publishNotification(currentState.copy())
                }
            }
        }
    }

    private suspend fun publishNotification(state: MusicNotificationState) {
        require(currentState !== state) // to avoid concurrency problems a copy is passed

        Timber.v("$TAG publish notification state=$state")
        issueNotification(state)
    }

    private suspend fun issueNotification(state: MusicNotificationState) {
        Timber.v("$TAG issue notification")
        val notification = notificationImpl.update(state)
        if (state.isPlaying) {
            startForeground(notification)
        } else {
            pauseForeground()
        }
    }

    private fun stopForeground() {
        if (!isForeground) {
            Timber.w("$TAG stop foreground request not not in foreground")
            return
        }
        Timber.v("$TAG stop foreground")

        service.stopForeground(true)
        notificationImpl.cancel()

        isForeground = false
    }

    private fun pauseForeground() {
        if (!isForeground) {
            Timber.w("$TAG pause foreground request not not in foreground")
            return
        }
        Timber.v("$TAG pause foreground")

        // state paused
        service.stopForeground(false)

        isForeground = false
    }

    private fun startForeground(notification: Notification) {
        if (isForeground) {
            Timber.w("$TAG start foreground request but already in foreground")
            return
        }
        Timber.v("$TAG start foreground")

        service.startForeground(INotification.NOTIFICATION_ID, notification)

        isForeground = true
    }

}
