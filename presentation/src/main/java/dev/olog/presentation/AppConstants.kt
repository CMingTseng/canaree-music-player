package dev.olog.presentation

import android.content.Context
import android.provider.MediaStore
import androidx.preference.PreferenceManager
import dev.olog.presentation.widgets.QuickActionView

object AppConstants {

    enum class ImageShape {
        RECTANGLE, ROUND
    }

    private const val TAG = "AppConstants"
    const val ACTION_CONTENT_VIEW = "$TAG.action.content.view"

    const val NO_IMAGE = "NO_IMAGE"

    var QUICK_ACTION = QuickActionView.Type.NONE
    var IMAGE_SHAPE = AppConstants.ImageShape.ROUND
    var SHOW_LOCKSCREEN_IMAGE = false

    const val PROGRESS_BAR_INTERVAL = 250

    const val UNKNOWN = MediaStore.UNKNOWN_STRING
    lateinit var UNKNOWN_ALBUM: String
    lateinit var UNKNOWN_ARTIST: String

    fun initialize(context: Context){
        UNKNOWN_ALBUM = context.getString(R.string.common_unknown_album)
        UNKNOWN_ARTIST = context.getString(R.string.common_unknown_artist)

        updateQuickAction(context)
        updateIconShape(context)
        updateLockscreenArtworkEnabled(context)
    }

    fun updateQuickAction(context: Context){
        QUICK_ACTION = getQuickAction(context)
    }

    fun updateIconShape(context: Context){
        IMAGE_SHAPE = getIconShape(context)
    }

    fun updateLockscreenArtworkEnabled(context: Context) {
        SHOW_LOCKSCREEN_IMAGE =
            getLockscreenArtworkEnabled(context)
    }

    private fun getLockscreenArtworkEnabled(context: Context): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return preferences.getBoolean(context.getString(R.string.prefs_lockscreen_artwork_key), false)
    }

    private fun getQuickAction(context: Context): QuickActionView.Type {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        val quickAction = preferences.getString(context.getString(R.string.prefs_quick_action_key), context.getString(R.string.prefs_quick_action_entry_value_hide))
        return when (quickAction) {
            context.getString(R.string.prefs_quick_action_entry_value_hide) -> QuickActionView.Type.NONE
            context.getString(R.string.prefs_quick_action_entry_value_play) -> QuickActionView.Type.PLAY
            else ->  QuickActionView.Type.SHUFFLE
        }
    }

    private fun getIconShape(context: Context): ImageShape {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        val shape = prefs.getString(context.getString(R.string.prefs_icon_shape_key), context.getString(R.string.prefs_icon_shape_rounded))!!
        return when (shape){
            context.getString(R.string.prefs_icon_shape_rounded) -> AppConstants.ImageShape.ROUND
            context.getString(R.string.prefs_icon_shape_square) -> AppConstants.ImageShape.RECTANGLE
            else -> throw IllegalArgumentException("image shape not valid=$shape")
        }
    }

}