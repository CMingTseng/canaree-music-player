package dev.olog.feature.service.music.scrobbling

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import de.umass.lastfm.Authenticator
import de.umass.lastfm.Caller
import de.umass.lastfm.Session
import de.umass.lastfm.Track
import de.umass.lastfm.scrobble.ScrobbleData
import dev.olog.shared.coroutines.autoDisposeJob
import dev.olog.domain.entity.UserCredentials
import dev.olog.domain.schedulers.Schedulers
import dev.olog.core.dagger.ServiceLifecycle
import dev.olog.feature.service.music.BuildConfig
import dev.olog.feature.service.music.model.MediaEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.logging.Level
import javax.inject.Inject

internal class LastFmService @Inject constructor(
    @ServiceLifecycle private val lifecycle: Lifecycle,
    private val schedulers: Schedulers
) {

    companion object {
        const val SCROBBLE_DELAY = 10L * 1000 // millis
    }

    private var session: Session? = null
    private var userCredentials: UserCredentials? = null

    private var scrobbleJob by autoDisposeJob()

    init {
        Caller.getInstance().userAgent = "dev.olog.msc"
        Caller.getInstance().logger.level = Level.OFF
    }

    fun tryAuthenticate(credentials: UserCredentials) {
        try {
            session = Authenticator.getMobileSession(
                credentials.username,
                credentials.password,
                BuildConfig.LAST_FM_KEY,
                BuildConfig.LAST_FM_SECRET
            )
            userCredentials = credentials
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    fun scrobble(entity: MediaEntity){
        if (session == null || userCredentials == null){
            return
        }

        scrobbleJob = lifecycle.coroutineScope.launch(schedulers.io) {
            delay(SCROBBLE_DELAY)
            val scrobbleData = entity.toScrollData()
            Track.scrobble(scrobbleData, session)
            Track.updateNowPlaying(scrobbleData, session)
        }
    }

    private fun MediaEntity.toScrollData(): ScrobbleData {
        return ScrobbleData(
            this.artist,
            this.title,
            (System.currentTimeMillis() / 1000).toInt(),
            this.duration.toInt(),
            this.album,
            null,
            null,
            this.trackNumber,
            null,
            true
        )
    }

}