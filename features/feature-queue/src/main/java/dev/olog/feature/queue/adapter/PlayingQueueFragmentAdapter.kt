package dev.olog.feature.queue.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import dev.olog.feature.presentation.base.adapter.*
import dev.olog.lib.media.MediaProvider
import dev.olog.feature.presentation.base.adapter.drag.IDragListener
import dev.olog.feature.presentation.base.adapter.drag.TouchableAdapter
import dev.olog.feature.presentation.base.loadSongImage
import dev.olog.feature.presentation.base.model.toDomain
import dev.olog.feature.queue.PlayingQueueFragmentViewModel
import dev.olog.feature.queue.R
import dev.olog.feature.queue.model.DisplayableQueueSong
import dev.olog.navigation.Navigator
import dev.olog.shared.android.extensions.textColorPrimary
import dev.olog.shared.android.extensions.textColorSecondary
import dev.olog.shared.swap
import kotlinx.android.synthetic.main.item_playing_queue.view.*

internal class PlayingQueueFragmentAdapter(
    private val mediaProvider: MediaProvider,
    private val navigator: Navigator,
    private val dragListener: IDragListener,
    private val viewModel: PlayingQueueFragmentViewModel

) : ObservableAdapter<DisplayableQueueSong>(DiffCallbackPlayingQueue),
    TouchableAdapter,
    CanShowIsPlaying by CanShowIsPlayingImpl() {

    private val moves = mutableListOf<Pair<Int, Int>>()

    override fun initViewHolderListeners(viewHolder: DataBoundViewHolder, viewType: Int) {
        viewHolder.setOnClickListener(this) { item, _, _ ->
            mediaProvider.skipToQueueItem(item.idInPlaylist)
        }

        viewHolder.setOnLongClickListener(this) { item, _, _ ->
            navigator.toDialog(item.mediaId.toDomain(), viewHolder.itemView, viewHolder.itemView)
        }
        viewHolder.setOnDragListener(R.id.dragHandle, dragListener)
        viewHolder.elevateSongOnTouch()
    }

    override fun bind(holder: DataBoundViewHolder, item: DisplayableQueueSong, position: Int) {
        holder.itemView.apply {
            transitionName = "playing queue ${item.mediaId}"
            isPlaying.toggleVisibility(item.mediaId == playingMediaId)
            holder.imageView!!.loadSongImage(item.mediaId.toDomain())
            index.text = item.relativePosition
            firstText.text = item.title
            secondText.text = item.subtitle
            explicit.onItemChanged(item.title)

            val textColor = calculateTextColor(context, item.relativePosition)
            index.setTextColor(textColor)
        }
    }

    private fun calculateTextColor(context: Context, positionInList: String): Int {
        return if (positionInList.startsWith("-")) context.textColorSecondary()
        else context.textColorPrimary()
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(
        holder: DataBoundViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val text = payloads.filterIsInstance<String>().firstOrNull()
        if (text != null) {
            val item = getItem(position)
            val textColor = calculateTextColor(
                holder.itemView.context,
                item.relativePosition
            )
            holder.itemView.index.updateText(text, textColor)
        }
        val payload = payloads.filterIsInstance<Boolean>().firstOrNull()
        if (payload != null) {
            holder.itemView.isPlaying.animateVisibility(payload)
        }
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun canInteractWithViewHolder(viewType: Int): Boolean {
        return viewType == R.layout.item_playing_queue
    }

    override fun onMoved(from: Int, to: Int) {
        mediaProvider.swap(from, to)
        backedList.swap(from, to)
        notifyItemMoved(from, to)
        moves.add(from to to)
    }

    override fun onSwipedRight(viewHolder: RecyclerView.ViewHolder) {
        mediaProvider.remove(viewHolder.adapterPosition)
    }

    override fun afterSwipeRight(viewHolder: RecyclerView.ViewHolder) {
        val position = viewHolder.adapterPosition
        backedList.removeAt(position)
        notifyItemRemoved(position)
        viewModel.recalculatePositionsAfterRemove(position)
    }

    override fun onClearView() {
        viewModel.recalculatePositionsAfterMove(moves.toList())
        moves.clear()
    }

}

