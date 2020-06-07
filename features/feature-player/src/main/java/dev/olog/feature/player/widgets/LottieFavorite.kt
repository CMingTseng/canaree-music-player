package dev.olog.feature.player.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.ColorFilter
import android.util.AttributeSet
import android.view.View
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dev.olog.domain.entity.favorite.FavoriteState
import dev.olog.feature.presentation.base.activity.HasSlidingPanel
import dev.olog.shared.android.dark.mode.isDarkMode
import dev.olog.shared.android.extensions.colorControlNormal
import dev.olog.shared.android.theme.themeManager
import dev.olog.shared.lazyFast

internal class LottieFavorite(
    context: Context,
    attrs: AttributeSet

) : LottieAnimationView(context, attrs) {

    private val slidingPanel by lazyFast { (context as HasSlidingPanel).getSlidingPanel() }
    private var isSlidingPanelExpanded = false

    private var state: FavoriteState? = null

    init {
        if (!isInEditMode){
            val isDarkMode = context.isDarkMode()
            val playerAppearance = context.themeManager.playerAppearance
            var useWhiteIcon = playerAppearance.isFullscreen

            useWhiteIcon = useWhiteIcon || isDarkMode

            val icon = when {
                useWhiteIcon -> "favorite_white"
                else -> "favorite"
            }
            setAnimation("$icon.json")

            scaleX = 1.15f
            scaleY = 1.15f
        } else {
            // design time
            setAnimation("favorite.json")

            scaleX = 1.15f
            scaleY = 1.15f
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode){
            isSlidingPanelExpanded = slidingPanel.state == BottomSheetBehavior.STATE_EXPANDED
            slidingPanel.addBottomSheetCallback(listener)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isSlidingPanelExpanded = false
        slidingPanel.removeBottomSheetCallback(listener)
    }

    private fun toggleFavorite(isFavorite: Boolean) {
        cancelAnimation()
        if (isFavorite) {
            progress = 1f
        } else {
            progress = 0f
        }
    }

    fun toggleFavorite(){
        this.state = this.state?.reverse()
        animateFavorite(this.state == FavoriteState.FAVORITE)
    }

    private fun animateFavorite(toFavorite: Boolean) {
        cancelAnimation()
        if (toFavorite) {
            progress = .35f
            resumeAnimation()
        } else {
            progress = 0f
        }
    }

    fun onNextState(favoriteEnum: FavoriteState) {
        if (this.state == favoriteEnum) {
            return
        }
        this.state = FavoriteState.valueOf(favoriteEnum.name)

        when (favoriteEnum) {
            FavoriteState.FAVORITE -> toggleFavorite(true)
            FavoriteState.NOT_FAVORITE -> toggleFavorite(false)
        }
    }

    private val listener = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            isSlidingPanelExpanded = slidingPanel.state == BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1f else 0.5f
        val color = if (enabled) {
            Color.TRANSPARENT
        } else {
            context.colorControlNormal()
        }
        val filter = SimpleColorFilter(color)
        val keyPath = KeyPath("**")
        val callback =
            LottieValueCallback<ColorFilter>(filter)
        addValueCallback(keyPath, LottieProperty.COLOR_FILTER, callback)
    }

}