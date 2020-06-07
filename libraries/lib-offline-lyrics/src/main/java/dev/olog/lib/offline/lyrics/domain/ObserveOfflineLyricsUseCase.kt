package dev.olog.lib.offline.lyrics.domain

import dev.olog.domain.gateway.OfflineLyricsGateway
import dev.olog.domain.gateway.track.TrackGateway
import dev.olog.domain.schedulers.Schedulers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class ObserveOfflineLyricsUseCase @Inject constructor(
    private val gateway: OfflineLyricsGateway,
    private val trackGateway: TrackGateway,
    private val schedulers: Schedulers,
    private val readPersistedLyricsUseCase: ReadPersistedLyricsUseCase

) {

    operator fun invoke(param: Long): Flow<String> {
        return gateway.observeLyrics(param)
            .map { lyrics -> mapLyrics(param, lyrics) }
            .flowOn(schedulers.io)
    }

    private fun mapLyrics(id: Long, lyrics: String): String {
        val song = trackGateway.getByParam(id)!!
        try {
            return readPersistedLyricsUseCase(song)
        } catch (ex: Exception) {
            Timber.e(ex)
            return lyrics
        }
    }

}