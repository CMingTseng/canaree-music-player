package dev.olog.presentation.createplaylist

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.olog.domain.entity.PlaylistType
import dev.olog.presentation.R
import dev.olog.feature.presentation.base.activity.BaseFragment
import dev.olog.feature.presentation.base.dialog.TextViewDialog
import dev.olog.feature.presentation.base.FloatingWindow
import dev.olog.feature.presentation.base.extensions.*
import dev.olog.feature.presentation.base.utils.hideIme
import dev.olog.feature.presentation.base.widget.fastscroller.WaveSideBarView
import dev.olog.scrollhelper.layoutmanagers.OverScrollLinearLayoutManager
import dev.olog.shared.TextUtils
import dev.olog.shared.lazyFast
import kotlinx.android.synthetic.main.fragment_create_playlist.*
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class CreatePlaylistFragment : BaseFragment(),
    FloatingWindow {

    companion object {
        val TAG = CreatePlaylistFragment::class.java.name
        const val ARGUMENT_PLAYLIST_TYPE = "playlist_type"

        @JvmStatic
        fun newInstance(type: PlaylistType): CreatePlaylistFragment {
            return CreatePlaylistFragment().withArguments(
                ARGUMENT_PLAYLIST_TYPE to type.ordinal
            )
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<CreatePlaylistFragmentViewModel> {
        viewModelFactory
    }

    private val adapter by lazyFast {
        CreatePlaylistFragmentAdapter(viewModel)
    }

    private var toast: Toast? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.layoutManager = OverScrollLinearLayoutManager(requireContext())
        list.adapter = adapter
        list.setHasFixedSize(true)

        viewModel.observeSelectedCount()
            .onEach { size ->
                val text = when (size) {
                    0 -> getString(R.string.popup_new_playlist)
                    else -> resources.getQuantityString(
                        R.plurals.playlist_tracks_chooser_count,
                        size,
                        size
                    )
                }
                header.text = text
                fab.toggleVisibility(size > 0)
            }.launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.data
            .onEach {
                restoreUpperWidgetsTranslation()
                adapter.suspendSubmitList(it)
                list.awaitAnimationEnd()
                emptyStateText.isVisible = it.isEmpty()
                sidebar.onDataChanged(it)
            }.launchIn(viewLifecycleOwner.lifecycleScope)

        sidebar.scrollableLayoutId = R.layout.item_create_playlist

        editText.afterTextChange()
            .filter { it.isBlank() || it.trim().length >= 2 }
            .debounce(250)
            .onEach { viewModel.updateFilter(it) }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        sidebar.setListener(letterTouchListener)
        fab.setOnClickListener { showCreateDialog() }
        back.setOnClickListener {
            editText.hideIme()
            requireActivity().onBackPressed()
        }
        filterList.setOnClickListener {
            filterList.toggleSelected()
            viewModel.toggleShowOnlyFiltered()

            toast?.cancel()

            if (filterList.isSelected) {
                toast = requireActivity().toast(R.string.playlist_tracks_chooser_show_only_selected)
            } else {
                toast = requireActivity().toast(R.string.playlist_tracks_chooser_show_all)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sidebar.setListener(null)
        fab.setOnClickListener(null)
        back.setOnClickListener(null)
        filterList.setOnClickListener(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        toast?.cancel()
        list.adapter = null
    }

    private fun showCreateDialog() {
        TextViewDialog(
            requireActivity(),
            getString(R.string.popup_new_playlist),
            null
        )
            .addTextView(customizeWrapper = {
                hint = getString(R.string.new_playlist_hint)
            })
            .show(
                positiveAction = TextViewDialog.Action(getString(R.string.popup_positive_ok)) {
                    val text = it[0].editableText.toString()
                    if (text.isNotBlank()){
                        viewModel.savePlaylist(text)
                    } else {
                        false
                    }
                }, dismissAction = {
                    dismiss()
                    requireActivity().onBackPressed()
                }
            )
    }

    private val letterTouchListener = WaveSideBarView.OnTouchLetterChangeListener { letter ->
        list.stopScroll()

        val position = when (letter) {
            TextUtils.MIDDLE_DOT -> -1
            "#" -> 0
            "?" -> adapter.lastIndex()
            else -> adapter.indexOf { item ->
                if (item.title.isBlank()) {
                    false
                } else {
                    "${item.title[0]}".toUpperCase() == letter
                }
            }
        }
        if (position != -1) {
            val layoutManager = list.layoutManager as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(position, 0)
        }
    }

    override fun provideLayoutId(): Int = R.layout.fragment_create_playlist
}