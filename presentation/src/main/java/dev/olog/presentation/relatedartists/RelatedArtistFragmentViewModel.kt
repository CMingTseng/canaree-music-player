package dev.olog.presentation.relatedartists

import android.content.Context
import android.content.res.Resources
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.olog.domain.entity.track.Artist
import dev.olog.domain.interactor.GetItemTitleUseCase
import dev.olog.domain.interactor.ObserveRelatedArtistsUseCase
import dev.olog.domain.schedulers.Schedulers
import dev.olog.feature.presentation.base.model.PresentationId
import dev.olog.presentation.R
import dev.olog.feature.presentation.base.model.DisplayableAlbum
import dev.olog.feature.presentation.base.model.presentationId
import dev.olog.feature.presentation.base.model.toDomain
import dev.olog.shared.coroutines.mapListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class RelatedArtistFragmentViewModel @ViewModelInject constructor(
    @Assisted private val bundle: SavedStateHandle,
    @ApplicationContext context: Context,
    useCase: ObserveRelatedArtistsUseCase,
    getItemTitleUseCase: GetItemTitleUseCase,
    schedulers: Schedulers

) : ViewModel() {

    private val mediaId: PresentationId.Category
        get() = bundle.get(RelatedArtistFragment.ARGUMENTS_MEDIA_ID)!!

    val itemOrdinal = mediaId.category.ordinal // TODO try to remove ordinal

    val data: Flow<List<DisplayableAlbum>> = useCase(mediaId.toDomain())
        .mapListItem { it.toRelatedArtist(context.resources) }
        .flowOn(schedulers.io)

    val title: Flow<String> = getItemTitleUseCase(mediaId.toDomain())
        .flowOn(schedulers.io)


    private fun Artist.toRelatedArtist(resources: Resources): DisplayableAlbum {
        val songs =
            resources.getQuantityString(R.plurals.common_plurals_song, this.songs, this.songs)

        return DisplayableAlbum(
            type = R.layout.item_related_artist,
            mediaId = presentationId,
            title = this.name,
            subtitle = songs
        )
    }

}