package dev.olog.feature.service.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import dev.olog.core.dagger.FeatureScope
import dev.olog.core.dagger.ServiceContext
import dev.olog.core.dagger.ServiceLifecycle
import dev.olog.feature.service.music.EventDispatcher.Event
import timber.log.Timber
import javax.inject.Inject

@FeatureScope
internal class Noisy @Inject constructor(
    @ServiceLifecycle lifecycle: Lifecycle,
    @ServiceContext private val context: Context,
    private val eventDispatcher: EventDispatcher

) : DefaultLifecycleObserver {

    companion object {
        @JvmStatic
        private val TAG = "SM:${Noisy::class.java.simpleName}"
    }

    private val noisyFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

    private var registered: Boolean = false

    init {
        lifecycle.addObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        unregister()
    }

    fun register() {
        if (registered){
            Timber.w("$TAG trying to re-register")
            return
        }
        Timber.v("$TAG register")
        context.registerReceiver(receiver, noisyFilter)
        registered = true
    }

    fun unregister() {
        if (!registered) {
            Timber.w("$TAG trying to unregister but never registered")
            return
        }

        Timber.v("$TAG unregister")
        context.unregisterReceiver(receiver)
        registered = false
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                Timber.v("$TAG on receiver noisy broadcast")
                eventDispatcher.dispatchEvent(Event.PLAY_PAUSE)
            }

        }
    }

}
