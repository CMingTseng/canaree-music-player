package dev.olog.lib.analytics

import dagger.Binds
import dagger.Module
import dev.olog.lib.analytics.tracker.FirebaseTracker
import javax.inject.Singleton

@Module
abstract class AnalyticsModule {

    @Binds
    @Singleton
    internal abstract fun provideTrackerFacade(impl: FirebaseTracker): TrackerFacade

}