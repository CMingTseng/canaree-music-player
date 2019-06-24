package dev.olog.msc.app.shortcuts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import dev.olog.media.MusicConstants
import dev.olog.msc.R
import dev.olog.msc.presentation.main.MainActivity
import dev.olog.msc.presentation.shortcuts.ShortcutsActivity
import dev.olog.msc.presentation.shortcuts.playlist.chooser.PlaylistChooserActivity
import dev.olog.presentation.AppConstants
import dev.olog.shared.isNougat_MR1

@RequiresApi(Build.VERSION_CODES.N_MR1)
open class AppShortcutsImpl25(
        context: Context

) : BaseAppShortcuts(context) {

    protected val shortcutManager : ShortcutManager = context.getSystemService()!!

    init {
        shortcutManager.removeAllDynamicShortcuts()
        shortcutManager.addDynamicShortcuts(listOf(
                playlistChooser(), search(), shuffle(), play()
        ))
    }

    override fun disablePlay(){
        if (isNougat_MR1()){
            shortcutManager.removeDynamicShortcuts(listOf(Shortcuts.PLAY))
        }
    }

    override fun enablePlay(){
        if (isNougat_MR1()){
            shortcutManager.addDynamicShortcuts(listOf(play()))
        }
    }

    private fun search(): ShortcutInfo {
        return ShortcutInfo.Builder(context, Shortcuts.SEARCH)
                .setShortLabel(context.getString(R.string.shortcut_search))
                .setIcon(Icon.createWithResource(context, R.drawable.shortcut_search))
                .setIntent(createSearchIntent())
                .build()
    }

    private fun play(): ShortcutInfo {
        return ShortcutInfo.Builder(context, Shortcuts.PLAY)
                .setShortLabel(context.getString(R.string.shortcut_play))
                .setIcon(Icon.createWithResource(context, R.drawable.shortcut_play))
                .setIntent(createPlayIntent())
                .build()
    }

    private fun shuffle(): ShortcutInfo {
        return ShortcutInfo.Builder(context, Shortcuts.SHUFFLE)
                .setShortLabel(context.getString(R.string.shortcut_shuffle))
                .setIcon(Icon.createWithResource(context, R.drawable.shortcut_shuffle))
                .setIntent(createShuffleIntent())
                .build()
    }

    private fun playlistChooser(): ShortcutInfo {
        return ShortcutInfo.Builder(context, Shortcuts.PLAYLIST_CHOOSER)
                .setShortLabel(context.getString(R.string.shortcut_playlist_chooser))
                .setIcon(Icon.createWithResource(context, R.drawable.shortcut_playlist_add))
                .setIntent(createPlaylistChooserIntent())
                .build()
    }

    private fun createSearchIntent(): Intent {
        val intent = Intent(context, MainActivity::class.java)
        intent.action = Shortcuts.SEARCH
        return intent
    }

    private fun createPlayIntent(): Intent {
        val intent = Intent(context, ShortcutsActivity::class.java)
        intent.action = MusicConstants.ACTION_PLAY
        return intent
    }

    private fun createShuffleIntent(): Intent {
        val intent = Intent(context, ShortcutsActivity::class.java)
        intent.action = MusicConstants.ACTION_SHUFFLE
        return intent
    }

    private fun createPlaylistChooserIntent(): Intent {
        val intent = Intent(context, PlaylistChooserActivity::class.java)
        intent.action = Shortcuts.PLAYLIST_CHOOSER
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        return intent
    }

}