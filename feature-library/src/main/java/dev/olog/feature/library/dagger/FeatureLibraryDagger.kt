package dev.olog.feature.library.dagger

import androidx.fragment.app.Fragment
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import dev.olog.feature.library.folder.tree.FolderTreeFragment
import dev.olog.feature.library.library.LibraryFragment
import dev.olog.feature.library.tab.TabFragment
import dev.olog.feature.presentation.base.dagger.ScreenScope
import dev.olog.navigation.dagger.FragmentScreenKey
import dev.olog.navigation.screens.FragmentScreen

class FeatureLibraryDagger {

    @Module
    abstract class AppModule {

        @ContributesAndroidInjector
        @ScreenScope
        internal abstract fun provideCategoriesFragment(): LibraryFragment

        @ContributesAndroidInjector(modules = [TabFragmentModule::class])
        @ScreenScope
        internal abstract fun provideTabFragment(): TabFragment

        @ContributesAndroidInjector(modules = [FolderTreeFragmentModule::class])
        @ScreenScope
        internal abstract fun provideFolderTreeFragment(): FolderTreeFragment

        companion object {

            @Provides
            @IntoMap
            @FragmentScreenKey(FragmentScreen.LIBRARY_TRACKS)
            internal fun provideLibraryTracksFragment(): Fragment {
                return LibraryFragment.newInstance(false)
            }

            @Provides
            @IntoMap
            @FragmentScreenKey(FragmentScreen.LIBRARY_PODCAST)
            internal fun provideLibraryPodcastFragment(): Fragment {
                return LibraryFragment.newInstance(true)
            }

        }

    }

}