package dev.olog.lib.image.loader.fetcher

import android.content.Context
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import dev.olog.shared.coroutines.DispatcherScope
import dev.olog.domain.MediaId
import dev.olog.domain.MediaIdCategory
import dev.olog.domain.entity.track.Song
import dev.olog.domain.gateway.track.TrackGateway
import dev.olog.domain.schedulers.Schedulers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import timber.log.Timber
import java.io.InputStream

class GlideOriginalImageFetcher(
    private val context: Context,
    private val mediaId: MediaId,
    private val trackGateway: TrackGateway,
    schedulers: Schedulers
) : DataFetcher<InputStream> {

    private val scope by DispatcherScope(schedulers.io)

    override fun getDataClass(): Class<InputStream> = InputStream::class.java
    override fun getDataSource(): DataSource = DataSource.LOCAL

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        scope.launch {
            val id = getId()
            if (id == -1L) {
                callback.onLoadFailed(Exception("item not found for id$id"))
                return@launch
            }

            val song: Song? = when (mediaId) {
                is MediaId.Track -> trackGateway.getByParam(id)
                is MediaId.Category -> {
                    if (mediaId.category == MediaIdCategory.ALBUMS) {
                        trackGateway.getByAlbumId(id)
                    } else {
                        callback.onLoadFailed(IllegalArgumentException("not a valid media id=$mediaId"))
                        return@launch
                    }
                }
            }
            yield()

            if (song == null) {
                callback.onLoadFailed(IllegalArgumentException("track not found for id $id"))
                return@launch
            }
            try {
                val stream = OriginalImageFetcher.loadImage(context, song)
                callback.onDataReady(stream)
            } catch (ex: Exception) {
                Timber.w(ex)
                callback.onLoadFailed(RuntimeException(ex))
            }
        }
    }



    private fun getId(): Long {
        return when (mediaId) {
            is MediaId.Track -> mediaId.id.toLong()
            is MediaId.Category -> mediaId.categoryId.toLong()
        }
    }

    override fun cleanup() {

    }

    override fun cancel() {
        scope.cancel()
    }

}