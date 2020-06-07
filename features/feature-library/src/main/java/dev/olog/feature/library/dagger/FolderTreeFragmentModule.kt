package dev.olog.feature.library.dagger

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import dev.olog.feature.library.folder.tree.FolderTreeFragmentViewModel
import dev.olog.feature.presentation.base.dagger.ViewModelKey

@Module
internal abstract class FolderTreeFragmentModule {

    @Binds
    @IntoMap
    @ViewModelKey(FolderTreeFragmentViewModel::class)
    internal abstract fun provideViewModel(viewModel: FolderTreeFragmentViewModel): ViewModel

}