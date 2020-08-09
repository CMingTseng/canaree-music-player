package dev.olog.feature.detail.adapter

import dev.olog.feature.presentation.base.adapter.*
import dev.olog.feature.presentation.base.loadAlbumImage
import dev.olog.feature.presentation.base.model.DisplayableAlbum
import dev.olog.navigation.Navigator
import dev.olog.feature.presentation.base.model.toDomain
import kotlinx.android.synthetic.main.item_detail_related_artist.view.*

internal class DetailRelatedArtistsAdapter(
    private val navigator: Navigator
) : ObservableAdapter<DisplayableAlbum>(DiffCallbackDisplayableAlbum) {

    override fun initViewHolderListeners(viewHolder: DataBoundViewHolder, viewType: Int) {
        viewHolder.setOnClickListener(this) { item, _, view ->
            navigator.toDetailFragment(item.mediaId.toDomain(), view)
        }
        viewHolder.setOnLongClickListener(this) { item, _, _ ->
            navigator.toDialog(item.mediaId.toDomain(), viewHolder.itemView, viewHolder.itemView)
        }
        viewHolder.elevateAlbumOnTouch()
    }

    override fun bind(holder: DataBoundViewHolder, item: DisplayableAlbum, position: Int) {
        holder.itemView.apply {
            transitionName = "detail related ${item.mediaId}"
            holder.imageView!!.loadAlbumImage(item.mediaId.toDomain())
            firstText.text = item.title
            secondText.text = item.subtitle
            quickAction.setId(item.mediaId)
        }
    }
}