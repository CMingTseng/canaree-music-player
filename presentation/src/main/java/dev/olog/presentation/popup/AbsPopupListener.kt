package dev.olog.presentation.popup

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.text.parseAsHtml
import androidx.fragment.app.FragmentActivity
import dev.olog.domain.entity.PlaylistType
import dev.olog.domain.entity.track.Playlist
import dev.olog.domain.entity.track.Song
import dev.olog.domain.interactor.playlist.AddToPlaylistUseCase
import dev.olog.domain.interactor.playlist.GetPlaylistsUseCase
import dev.olog.domain.schedulers.Schedulers
import dev.olog.feature.presentation.base.extensions.toast
import dev.olog.feature.presentation.base.model.PresentationId
import dev.olog.feature.presentation.base.model.toDomain
import dev.olog.presentation.R
import dev.olog.navigation.Navigator
import dev.olog.shared.android.FileProvider
import dev.olog.shared.lazyFast
import dev.olog.shared.throwNotHandled
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

internal abstract class AbsPopupListener(
    protected val activity: FragmentActivity,
    getPlaylistBlockingUseCase: GetPlaylistsUseCase,
    private val addToPlaylistUseCase: AddToPlaylistUseCase,
    private val schedulers: Schedulers

) : PopupMenu.OnMenuItemClickListener {

    protected var container: View? = null
    protected var podcastPlaylist: Boolean = false

    val playlists: List<Playlist> by lazyFast {
        getPlaylistBlockingUseCase(
            if (podcastPlaylist) PlaylistType.PODCAST
            else PlaylistType.TRACK
        )
    }

    @SuppressLint("RxLeakedSubscription")
    protected fun onPlaylistSubItemClick(
        context: Context,
        itemId: Int,
        mediaId: PresentationId,
        listSize: Int,
        title: String
    ) {
        val playlist = playlists.find { it.id == itemId.toLong() } ?: return
        GlobalScope.launch { // TODO use a better scope
            try {
                addToPlaylistUseCase(playlist, mediaId.toDomain())
                createSuccessMessage(
                    context,
                    itemId.toLong(),
                    mediaId,
                    listSize,
                    title
                )
            } catch (ex: Exception){
                Timber.e(ex)
                createErrorMessage(context)
            }
        }
    }

    private suspend fun createSuccessMessage(
        context: Context,
        playlistId: Long,
        mediaId: PresentationId,
        listSize: Int,
        title: String
    ) = withContext(schedulers.main) {
        val playlist = playlists.first { it.id == playlistId }.title
        val message = when (mediaId) {
            is PresentationId.Track -> context.getString(R.string.added_song_x_to_playlist_y, title, playlist)
            is PresentationId.Category -> context.resources.getQuantityString(
                R.plurals.xx_songs_added_to_playlist_y,
                listSize,
                listSize,
                playlist
            )
        }
        context.toast(message)
    }

    private suspend fun createErrorMessage(context: Context) = withContext(schedulers.main) {
        context.toast(context.getString(R.string.popup_error_message))
    }

    protected fun share(activity: Activity, song: Song) {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        val uri = FileProvider.getUriForPath(activity, song.path)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.type = "audio/*"
        grantUriPermission(activity, intent, uri)
        try {
            if (intent.resolveActivity(activity.packageManager) != null) {
                val string = activity.getString(R.string.share_song_x, song.title)
                activity.startActivity(Intent.createChooser(intent, string.parseAsHtml()))
            } else {
                activity.toast(R.string.song_not_shareable)
            }
        } catch (ex: Exception) {
            Timber.e(ex)
            activity.toast(R.string.song_not_shareable)
        }
    }

    private fun grantUriPermission(context: Context, intent: Intent, uri: Uri){
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val resInfoList = context.packageManager.queryIntentActivities(intent, 0)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            context.grantUriPermission(
                packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    protected fun viewInfo(navigator: Navigator, mediaId: PresentationId) {
        navigator.toEditInfoFragment(mediaId.toDomain())
    }

    protected fun viewAlbum(navigator: Navigator, mediaId: PresentationId.Category) {
        navigator.toDetailFragment(mediaId.toDomain(), container)
    }

    protected fun viewArtist(navigator: Navigator, mediaId: PresentationId.Category) {
        navigator.toDetailFragment(mediaId.toDomain(), container)
    }

    protected fun setRingtone(navigator: Navigator, mediaId: PresentationId, song: Song) {
        when (mediaId) {
            is PresentationId.Track -> navigator.toSetRingtoneDialog(mediaId.toDomain(), song.title, song.artist)
            is PresentationId.Category -> throwNotHandled(mediaId)
        }
    }


}