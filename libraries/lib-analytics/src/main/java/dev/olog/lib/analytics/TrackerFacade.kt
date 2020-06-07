package dev.olog.lib.analytics

import android.os.Bundle

interface TrackerFacade {

    fun trackScreen(name: String, bundle: Bundle?)

    fun trackServiceEvent(name: String, vararg args: Any?)

}