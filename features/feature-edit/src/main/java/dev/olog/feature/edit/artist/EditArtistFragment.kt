package dev.olog.feature.edit.artist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dev.olog.domain.MediaId
import dev.olog.feature.presentation.base.extensions.*
import dev.olog.feature.presentation.base.model.PresentationId
import dev.olog.feature.presentation.base.model.toPresentation
import dev.olog.feature.edit.BaseEditItemFragment
import dev.olog.feature.edit.EditItemViewModel
import dev.olog.feature.edit.R
import dev.olog.feature.edit.model.UpdateArtistInfo
import dev.olog.lib.audio.tagger.model.Tags
import dev.olog.feature.edit.model.UpdateResult
import dev.olog.shared.android.extensions.extractText
import dev.olog.shared.lazyFast
import kotlinx.android.synthetic.main.fragment_edit_artist.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class EditArtistFragment : BaseEditItemFragment() {

    companion object {
        const val TAG = "EditArtistFragment"
        const val ARGUMENTS_MEDIA_ID = "${TAG}_arguments_media_id"

        @JvmStatic
        fun newInstance(mediaId: MediaId.Category): EditArtistFragment {
            return EditArtistFragment().withArguments(
                ARGUMENTS_MEDIA_ID to mediaId.toPresentation()
            )
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<EditArtistFragmentViewModel> {
        viewModelFactory
    }

    private val editItemViewModel by activityViewModels<EditItemViewModel> {
        viewModelFactory
    }

    private val mediaId by lazyFast {
        getArgument<PresentationId.Category>(ARGUMENTS_MEDIA_ID)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.requestData(mediaId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        artist.afterTextChange()
            .map { it.isNotBlank() }
            .onEach { okButton.isEnabled = it }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        loadImage(mediaId)

        viewModel.observeData()
            .onEach {
                artist.setText(it.title)
                albumArtist.setText(it.albumArtist)
                val plural = if (it.isPodcast) {
                    R.plurals.edit_item_xx_episodes_will_be_updated
                } else {
                    R.plurals.edit_item_xx_tracks_will_be_updated
                }
                val text = resources.getQuantityString(plural, it.songs, it.songs)
                albumsUpdated.text = text
                podcast.isChecked = it.isPodcast
            }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        okButton.onClick { trySave() }
        cancelButton.setOnClickListener { dismiss() }
    }

    override fun onPause() {
        super.onPause()
        okButton.setOnClickListener(null)
        cancelButton.setOnClickListener(null)
    }

    private suspend fun trySave() {
        val result = editItemViewModel.updateArtist(
            UpdateArtistInfo(
                mediaId = mediaId,
                tags = Tags(
                    artist = artist.extractText().trim(),
                    albumArtist = albumArtist.extractText().trim()
                ),
                isPodcast = podcast.isChecked
            )
        )

        when (result) {
            UpdateResult.OK -> dismiss()
            UpdateResult.EMPTY_TITLE -> requireContext().toast(R.string.edit_artist_invalid_title)
            else -> {
            }
        }
    }

    override fun onLoaderCancelled() {
    }

    override fun provideLayoutId(): Int = R.layout.fragment_edit_artist
}