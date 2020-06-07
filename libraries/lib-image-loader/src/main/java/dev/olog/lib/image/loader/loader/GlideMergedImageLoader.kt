package dev.olog.lib.image.loader.loader

import android.content.Context
import android.net.Uri
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import dev.olog.domain.MediaId
import dev.olog.domain.MediaIdCategory.*
import dev.olog.domain.gateway.track.FolderGateway
import dev.olog.domain.gateway.track.GenreGateway
import dev.olog.domain.gateway.track.PlaylistGateway
import dev.olog.domain.prefs.AppPreferencesGateway
import dev.olog.domain.schedulers.Schedulers
import dev.olog.lib.image.loader.fetcher.GlideMergedImageFetcher
import java.io.InputStream
import javax.inject.Inject

private val allowedCategories = listOf(
    FOLDERS, PLAYLISTS, GENRES, PODCASTS_PLAYLIST
)

class GlideMergedImageLoader(
    private val context: Context,
    private val uriLoader: ModelLoader<Uri, InputStream>,
    private val folderGateway: FolderGateway,
    private val playlistGateway: PlaylistGateway,
    private val genreGateway: GenreGateway,
    private val prefsGateway: AppPreferencesGateway,
    private val schedulers: Schedulers
) : ModelLoader<MediaId.Category, InputStream> {

    override fun handles(mediaId: MediaId.Category): Boolean {
        return mediaId.category in allowedCategories
    }

    override fun buildLoadData(
        mediaId: MediaId.Category,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream>? {
        if (!prefsGateway.canAutoCreateImages()) {
//             skip
            return uriLoader.buildLoadData(Uri.EMPTY, width, height, options)
        }

        return ModelLoader.LoadData(
            MediaIdKey(mediaId),
            GlideMergedImageFetcher(
                context = context,
                mediaId = mediaId,
                folderGateway = folderGateway,
                playlistGateway = playlistGateway,
                genreGateway = genreGateway,
                schedulers = schedulers
            )
        )
    }

    class Factory @Inject constructor(
        private val context: Context,
        private val folderGateway: FolderGateway,
        private val playlistGateway: PlaylistGateway,
        private val genreGateway: GenreGateway,
        private val prefsGateway: AppPreferencesGateway,
        private val schedulers: Schedulers
    ) : ModelLoaderFactory<MediaId.Category, InputStream> {

        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<MediaId.Category, InputStream> {
            val uriLoader = multiFactory.build(Uri::class.java, InputStream::class.java)
            return GlideMergedImageLoader(
                context = context,
                uriLoader = uriLoader,
                folderGateway = folderGateway,
                playlistGateway = playlistGateway,
                genreGateway = genreGateway,
                prefsGateway = prefsGateway,
                schedulers = schedulers
            )
        }

        override fun teardown() {

        }
    }
}

