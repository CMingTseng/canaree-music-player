package dev.olog.msc.domain.interactor.all.sibling

import dev.olog.msc.domain.entity.PodcastAlbum
import dev.olog.msc.domain.executors.IoScheduler
import dev.olog.msc.domain.gateway.PodcastAlbumGateway
import dev.olog.msc.domain.interactor.base.ObservableUseCaseUseCaseWithParam
import dev.olog.msc.domain.interactor.item.GetPodcastAlbumUseCase
import dev.olog.msc.utils.MediaId
import io.reactivex.Observable
import javax.inject.Inject

class GetPodcastAlbumSiblingsByAlbumUseCase @Inject constructor(
        schedulers: IoScheduler,
        private val getAlbumUseCase: GetPodcastAlbumUseCase,
        private val albumGateway: PodcastAlbumGateway

) : ObservableUseCaseUseCaseWithParam<List<PodcastAlbum>, MediaId>(schedulers) {


    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun buildUseCaseObservable(mediaId: MediaId): Observable<List<PodcastAlbum>> {
        val albumId = mediaId.categoryValue.toLong()
        return getAlbumUseCase.execute(mediaId)
                .map { it.artistId }
                .flatMap { artistId ->
                    albumGateway.observeByArtist(artistId)
                            .map { it.filter { it.id != albumId } }
                }
    }

}