package dev.olog.presentation.relatedartists

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dev.olog.domain.MediaId
import dev.olog.presentation.R
import dev.olog.feature.presentation.base.activity.BaseFragment
import dev.olog.navigation.Navigator
import dev.olog.scrollhelper.layoutmanagers.OverScrollGridLayoutManager
import dev.olog.feature.presentation.base.extensions.getArgument
import dev.olog.feature.presentation.base.extensions.withArguments
import dev.olog.feature.presentation.base.model.toPresentation
import dev.olog.shared.lazyFast
import kotlinx.android.synthetic.main.fragment_related_artist.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class RelatedArtistFragment : BaseFragment() {

    companion object {
        @JvmStatic
        val TAG = RelatedArtistFragment::class.java.name
        const val ARGUMENTS_MEDIA_ID = "media_id"
        const val ARGUMENTS_TRANSITION = "transition"

        @JvmStatic
        fun newInstance(mediaId: MediaId.Category, transition: String): RelatedArtistFragment {
            return RelatedArtistFragment().withArguments(
                ARGUMENTS_MEDIA_ID to mediaId.toPresentation(),
                ARGUMENTS_TRANSITION to transition
            )
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    internal lateinit var navigator: Navigator

    private val adapter by lazyFast {
        RelatedArtistFragmentAdapter(navigator)
    }

    private val viewModel by viewModels<RelatedArtistFragmentViewModel> {
        viewModelFactory
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.transitionName = getArgument(ARGUMENTS_TRANSITION)

        list.layoutManager = OverScrollGridLayoutManager(requireContext(), 2)
        list.adapter = adapter
        list.setHasFixedSize(true)

        viewModel.data
            .onEach { adapter.submitList(it) }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.title
            .onEach { itemTitle ->
                val headersArray = resources.getStringArray(R.array.related_artists_header)
                val header = String.format(headersArray[viewModel.itemOrdinal], itemTitle)
                this.header.text = header
            }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        back.setOnClickListener { requireActivity().onBackPressed() }
    }

    override fun onPause() {
        super.onPause()
        back.setOnClickListener(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        list.adapter = null
    }

    override fun provideLayoutId(): Int = R.layout.fragment_related_artist
}