package dev.olog.feature.player.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import dev.olog.core.coroutines.viewScope
import dev.olog.domain.MediaId
import dev.olog.lib.image.loader.CoverUtils
import dev.olog.shared.android.dark.mode.isDarkMode
import dev.olog.shared.coroutines.autoDisposeJob
import dev.olog.shared.lazyFast
import io.alterac.blurkit.BlurKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import timber.log.Timber

internal class BlurredBackground(
    context: Context,
    attrs: AttributeSet
) : AppCompatImageView(context, attrs) {

    private var job by autoDisposeJob()

    companion object {
        private const val LIGHT_MODE_SIZE = 250
        private const val DARK_MODE_SIZE = 175
        private const val BLUR_RADIUS = 25
    }

    init {
        scaleType = ScaleType.CENTER_CROP
        adjustViewBounds = true
    }

    private val isDarkMode by lazyFast { context.isDarkMode() }

    fun loadImage(mediaId: MediaId, drawable: Drawable?) {
        if (drawable == null){
            return
        }
        job = viewScope.launchWhenAttached {
            try {
                loadImageInternal(mediaId, drawable.mutate())
            } catch (ex: Exception){
                Timber.e(ex)
            }
        }

    }

    @SuppressLint("ConcreteDispatcherIssue")
    private suspend fun loadImageInternal(
        mediaId: MediaId,
        drawable: Drawable
    ) = withContext(Dispatchers.Default){

        val size = if (isDarkMode) DARK_MODE_SIZE else LIGHT_MODE_SIZE

        val bitmap = if (drawable is LayerDrawable){
            CoverUtils.onlyGradient(context, mediaId).toBitmap(size, size)
        } else {
            drawable.toBitmap(size, size)
        }
        yield()
        val blurred = BlurKit.getInstance().blur(bitmap, BLUR_RADIUS).toDrawable(resources)
        yield()
        withContext(Dispatchers.Main) {
            background = blurred
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        job = null
    }

}