package dev.olog.feature.equalizer.widgets.croller

import android.content.Context
import android.util.AttributeSet

internal class CanareeCroller(
    context: Context,
    attrs: AttributeSet
) : Croller(context, attrs) {

    var onProgressChanged: ((Int) -> Unit)? = null

    var max = 1000
    var min = 0

    init {
        mProgressChangeListener = OnCrollerProgressChangedListener {
            onProgressChanged?.invoke(it * this.max / innerMax)
        }
    }

    fun setProgress(progress: Int) {
        innerProgress = progress * innerMax / max
    }

}