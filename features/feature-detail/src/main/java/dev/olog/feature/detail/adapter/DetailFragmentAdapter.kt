package dev.olog.feature.detail.adapter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.View
import androidx.core.text.parseAsHtml
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.olog.domain.entity.AutoPlaylist
import dev.olog.domain.entity.spotify.SpotifyTrack
import dev.olog.feature.presentation.base.adapter.*
import dev.olog.feature.presentation.base.model.*
import dev.olog.lib.media.MediaProvider
import dev.olog.feature.presentation.base.adapter.drag.IDragListener
import dev.olog.feature.presentation.base.adapter.drag.TouchableAdapter
import dev.olog.feature.detail.DetailFragmentHeaders
import dev.olog.feature.detail.DetailFragmentViewModel
import dev.olog.feature.detail.DetailFragmentViewModel.Companion.NESTED_SPAN_COUNT
import dev.olog.feature.detail.DetailSortDialog
import dev.olog.feature.detail.R
import dev.olog.feature.presentation.base.SetupNestedList
import dev.olog.feature.presentation.base.loadBigAlbumImage
import dev.olog.feature.presentation.base.loadSongImage
import dev.olog.navigation.Navigator
import dev.olog.shared.android.extensions.colorAccent
import dev.olog.shared.android.extensions.textColorPrimary
import dev.olog.shared.exhaustive
import dev.olog.shared.swap
import kotlinx.android.synthetic.main.item_detail_biography.view.*
import kotlinx.android.synthetic.main.item_detail_header.view.*
import kotlinx.android.synthetic.main.item_detail_header.view.title
import kotlinx.android.synthetic.main.item_detail_header_albums.view.*
import kotlinx.android.synthetic.main.item_detail_header_all_song.view.*
import kotlinx.android.synthetic.main.item_detail_podcast.view.*
import kotlinx.android.synthetic.main.item_detail_song.view.explicit
import kotlinx.android.synthetic.main.item_detail_song.view.firstText
import kotlinx.android.synthetic.main.item_detail_song.view.secondText
import kotlinx.android.synthetic.main.item_detail_song_most_played.view.index
import kotlinx.android.synthetic.main.item_detail_song_most_played.view.isPlaying
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class DetailFragmentAdapter(
    private val mediaId: PresentationId.Category,
    private val setupNestedList: SetupNestedList,
    private val navigator: Navigator,
    private val mediaProvider: MediaProvider,
    private val viewModel: DetailFragmentViewModel,
    private val dragListener: IDragListener,
    private val afterImageLoad: () -> Unit
) : ObservableAdapter<DisplayableItem>(DiffCallbackDetailDisplayableItem),
    TouchableAdapter,
    CanShowIsPlaying by CanShowIsPlayingImpl() {

    private var podcastPositions = emptyMap<Long, Int>()

    private val headers by lazy { currentList.indexOfFirst { it is DisplayableTrack } }

    override fun initViewHolderListeners(viewHolder: DataBoundViewHolder, viewType: Int) {
        when (viewType) {

            R.layout.item_detail_list_most_played,
            R.layout.item_detail_list_recently_added,
            R.layout.item_detail_list_related_artists,
            R.layout.item_detail_list_albums,
            R.layout.item_detail_list_spotify_albums,
            R.layout.item_detail_list_spotify_singles -> {
                setupNestedList.setupNestedList(viewType, viewHolder.itemView as RecyclerView)
            }
            R.layout.item_detail_podcast -> {
                viewHolder.setOnClickListener(this) { item, _, _ ->
                    require(item is DisplayableTrack)
                    viewModel.detailSortDataUseCase(item.mediaId) {
                        mediaProvider.playFromMediaId(item.mediaId.toDomain(), viewModel.getFilter(), it)
                    }
                }
                viewHolder.setOnLongClickListener(this) { item, _, _ ->
                    navigator.toDialog(item.mediaId.toDomain(), viewHolder.itemView, viewHolder.itemView)
                }
            }
            R.layout.item_detail_song_with_track_spotify -> {
                viewHolder.setOnClickListener(this) { item, _, _ ->
                    require(item is DisplayableTrack)
                    if (item.mediaId.id != SpotifyTrack.INVALID_PREVIEW_URL) {
                        mediaProvider.playSpotifyPreview(item.mediaId.toDomain())
                    }
                }
            }
            R.layout.item_detail_song,
            R.layout.item_detail_song_with_track,
            R.layout.item_detail_song_with_drag_handle,
            R.layout.item_detail_song_with_track_and_image -> {
                viewHolder.setOnClickListener(this) { item, _, _ ->
                    require(item is DisplayableTrack)
                    viewModel.detailSortDataUseCase(item.mediaId) {
                        mediaProvider.playFromMediaId(item.mediaId.toDomain(), viewModel.getFilter(), it)
                    }
                }
                viewHolder.setOnLongClickListener(this) { item, _, _ ->
                    navigator.toDialog(item.mediaId.toDomain(), viewHolder.itemView, viewHolder.itemView)
                }
                viewHolder.setOnClickListener(R.id.more, this) { item, _, view ->
                    navigator.toDialog(item.mediaId.toDomain(), view, viewHolder.itemView)
                }

                viewHolder.setOnDragListener(R.id.dragHandle, dragListener)
            }
            R.layout.item_detail_shuffle -> {
                viewHolder.setOnClickListener(this) { _, _, _ ->
                    mediaProvider.shuffle(mediaId.toDomain(), viewModel.getFilter())
                }
            }

            R.layout.item_detail_header_recently_added -> {
                viewHolder.setOnClickListener(this) { _, _, _ ->
                    navigator.toRecentlyAdded(mediaId.toDomain(), viewHolder.itemView)
                }
            }
            R.layout.item_detail_header -> {

                viewHolder.setOnClickListener(this) { item, _, _ ->
                    if (item.mediaId == DetailFragmentHeaders.RELATED_ARTISTS_SEE_ALL) {
                        navigator.toRelatedArtists(mediaId.toDomain(), viewHolder.itemView)
                    }
                }
            }

            R.layout.item_detail_header_all_song -> {
                viewHolder.setOnClickListener(R.id.sort, this) { _, _, view ->
                    viewModel.observeSortOrder { currentSortType ->
                        DetailSortDialog().show(view, mediaId, currentSortType) { newSortType ->
                            viewModel.updateSortOrder(newSortType)
                        }
                    }
                }
                viewHolder.setOnClickListener(R.id.sortImage, this) { _, _, _ ->
                    viewModel.toggleSortArranging()
                }
            }
        }

        when (viewType) {
            R.layout.item_detail_song,
            R.layout.item_detail_song_with_track,
            R.layout.item_detail_song_with_drag_handle -> viewHolder.elevateSongOnTouch()
        }
    }

    override fun onViewAttachedToWindow(holder: DataBoundViewHolder) {
        super.onViewAttachedToWindow(holder)

        val view = holder.itemView

        when (holder.itemViewType) {
            R.layout.item_detail_list_recently_added,
            R.layout.item_detail_list_most_played -> {
                val list = holder.itemView as RecyclerView
                val layoutManager = list.layoutManager as GridLayoutManager
                val adapter = list.adapter as ObservableAdapter<*>
                adapter.observeData
                    .onEach { updateNestedSpanCount(layoutManager, it.size) }
                    .launchIn(holder.lifecycleScope)
            }
            R.layout.item_detail_header_all_song -> {
                val sortText = holder.itemView.sort
                val sortImage = holder.itemView.sortImage

                // don't allow sorting on podcast
                sortText.isVisible = !mediaId.isAnyPodcast && !AutoPlaylist.isAutoPlaylist(mediaId.categoryId)
                sortImage.isVisible = !mediaId.isAnyPodcast && !AutoPlaylist.isAutoPlaylist(mediaId.categoryId)

                viewModel.observeSorting()
                    .onEach { view.sortImage.update(it) }
                    .launchIn(holder.lifecycleScope)

                if (viewModel.showSortByTutorialIfNeverShown()) {
//                    TutorialTapTarget.sortBy(sortText, sortImage) TODO tutorial
                }
            }
            R.layout.item_detail_biography -> {
                viewModel.biography
                    .map { it?.parseAsHtml() }
                    .onEach { view.biography.text = it }
                    .launchIn(holder.lifecycleScope)
            }
        }
    }

    private fun updateNestedSpanCount(layoutManager: GridLayoutManager, size: Int) {
        layoutManager.spanCount = when {
            size == 0 -> 1
            size < NESTED_SPAN_COUNT -> size
            else -> NESTED_SPAN_COUNT
        }
    }

    override fun onBindViewHolder(
        holder: DataBoundViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val item = getItem(position)

        val payload = payloads.filterIsInstance<List<String>>().firstOrNull()
        if (payload != null) {
            holder.itemView.apply {
                title.text = payload[0]
                subtitle.text = payload[1]
            }
        }
        val currentPayload = payloads.filterIsInstance<Boolean>().firstOrNull()
        if (currentPayload != null) {
            holder.itemView.isPlaying.animateVisibility(currentPayload)
            if (item is DisplayableTrack) {
                bindPodcastProgressBarTint(holder.itemView, item)
            }
        }

        val updatePodcastPosition = payloads.filterIsInstance<Unit>().firstOrNull()
        if (updatePodcastPosition != null && item is DisplayableTrack) {
            bindPodcast(holder.itemView, item)
        }

        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun bind(holder: DataBoundViewHolder, item: DisplayableItem, position: Int) {
        holder.itemView.transitionName = "detail ${item.mediaId}"
        when (item){
            is DisplayableTrack -> bindTrack(holder, item)
            is DisplayableHeader -> bindHeader(holder, item)
            is DisplayableNestedListPlaceholder -> {}
            is DisplayableAlbum -> {}
        }.exhaustive
    }

    private fun bindTrack(holder: DataBoundViewHolder, item: DisplayableTrack){
        holder.itemView.apply {
            isPlaying.toggleVisibility(item.mediaId == playingMediaId)

            holder.imageView?.loadSongImage(item.mediaId.toDomain())
            firstText.text = item.title
            secondText?.text = item.subtitle
            explicit?.onItemChanged(item.title, item.isExplicit)

            if (mediaId.isAnyPodcast) {
                bindPodcast(this, item)
                bindPodcastProgressBarTint(this, item)
            }
            // show as disabled when current has an invalid previewUrl
            firstText.isEnabled = item.mediaId.id != SpotifyTrack.INVALID_PREVIEW_URL
            index?.isEnabled = item.mediaId.id != SpotifyTrack.INVALID_PREVIEW_URL

        }
        when (holder.itemViewType){
            R.layout.item_detail_song_with_track,
            R.layout.item_detail_song_with_track_spotify,
            R.layout.item_detail_song_with_track_and_image -> {
                val trackNumber = if (item.idInPlaylist < 1){
                    "-"
                } else item.idInPlaylist.toString()
                holder.itemView.index.text = trackNumber
            }
        }
    }

    private fun bindHeader(holder: DataBoundViewHolder, item: DisplayableHeader){
        when (holder.itemViewType){
            R.layout.item_detail_image -> {
                holder.imageView!!.post { afterImageLoad() }
                holder.imageView!!.loadBigAlbumImage(mediaId.toDomain())
                holder.itemView.title.text = item.title
                holder.itemView.subtitle.text = item.subtitle
            }
            R.layout.item_detail_song_footer,
            R.layout.item_detail_header,
            R.layout.item_detail_header_albums,
            R.layout.item_detail_header_recently_added,
            R.layout.item_detail_image -> {
                holder.itemView.apply {
                    title.text = item.title
                    subtitle?.text = item.subtitle
                    seeMore?.isVisible = item.visible
                }
            }
            R.layout.item_detail_header_all_song -> {
                holder.itemView.apply {
                    title.text = item.title
                    sort.text = item.subtitle
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindPodcast(view: View, item: DisplayableTrack) {
        val duration = item.duration.toInt()
        val progress = podcastPositions[item.mediaId.id.toLong()] ?: 0
        view.progressBar.max = duration
        view.progressBar.progress = progress

        val percentage = (progress.toFloat() / duration.toFloat() * 100f).toInt()
        view.percentage?.text = "$percentage%"
    }

    private fun bindPodcastProgressBarTint(view: View, item: DisplayableTrack) {
        val color = if (item.mediaId == playingMediaId) {
            view.context.colorAccent()
        } else {
            view.context.textColorPrimary()
        }
        view.progressBar.progressTintList = ColorStateList.valueOf(color)
    }

    fun updatePodcastPositions(positions: Map<Long, Int>) {
        this.podcastPositions = positions
        for (index in currentList.indices) {
            notifyItemChanged(index, Unit)
        }
    }

    val canSwipeRight: Boolean
        get() {
            val isPlaylist = mediaId.category == PresentationIdCategory.PLAYLISTS
            val isPodcastPlaylist = mediaId.category == PresentationIdCategory.PODCASTS_PLAYLIST
            if (isPlaylist || isPodcastPlaylist) {
                val playlistId = mediaId.categoryId.toLong()
                return playlistId != AutoPlaylist.LAST_ADDED.id || !AutoPlaylist.isAutoPlaylist(
                    playlistId
                )
            }
            return false
        }

    override fun canInteractWithViewHolder(viewType: Int): Boolean {
        return viewType == R.layout.item_detail_song ||
                viewType == R.layout.item_detail_song_with_drag_handle ||
                viewType == R.layout.item_detail_song_with_track ||
                viewType == R.layout.item_detail_song_with_track_and_image
    }

    override fun onClearView() {
        viewModel.processMove()
    }

    override fun onMoved(from: Int, to: Int) {
        val realFrom = from - headers
        val realTo = to - headers
        backedList.swap(from, to)
        notifyItemMoved(from, to)
        viewModel.addMove(realFrom, realTo)
    }

    override fun onSwipedRight(viewHolder: RecyclerView.ViewHolder) {
        val position = viewHolder.adapterPosition
        val item = getItem(position)
        backedList.removeAt(position)
        notifyItemRemoved(position)
        viewModel.removeFromPlaylist(item)
    }

    override fun afterSwipeRight(viewHolder: RecyclerView.ViewHolder) {

    }

    override fun onSwipedLeft(viewHolder: RecyclerView.ViewHolder) {
        val item = getItem(viewHolder.adapterPosition)
        require(item is DisplayableTrack)
        mediaProvider.addToPlayNext(item.mediaId.toDomain())
    }

    override fun afterSwipeLeft(viewHolder: RecyclerView.ViewHolder) {
        notifyItemChanged(viewHolder.adapterPosition)
    }


}

object DiffCallbackDetailDisplayableItem : DiffUtil.ItemCallback<DisplayableItem>() {

    override fun areItemsTheSame(oldItem: DisplayableItem, newItem: DisplayableItem): Boolean {
        return oldItem.mediaId == newItem.mediaId
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: DisplayableItem, newItem: DisplayableItem): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: DisplayableItem, newItem: DisplayableItem): Any? {
        if (newItem.type == R.layout.item_detail_image){
            require(newItem is DisplayableHeader)
            return listOf(
                newItem.title,
                newItem.subtitle
            )
        }
        return null
    }
}