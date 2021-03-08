package com.adkom666.shrednotes.di.module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.adkom666.shrednotes.di.viewmodel.ViewModelFactory
import com.adkom666.shrednotes.di.viewmodel.ViewModelKey
import com.adkom666.shrednotes.ui.exercises.ExerciseViewModel
import com.adkom666.shrednotes.ui.exercises.ExercisesViewModel
import com.adkom666.shrednotes.ui.notes.NotesViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import kotlinx.coroutines.ExperimentalCoroutinesApi

@Suppress("unused", "UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module(includes = [RepositoryModule::class])
abstract class ViewModelModule {

    @Binds
    abstract fun viewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @ExperimentalCoroutinesApi
    @Binds
    @IntoMap
    @ViewModelKey(NotesViewModel::class)
    abstract fun notesViewModel(viewModel: NotesViewModel): ViewModel

    @ExperimentalCoroutinesApi
    @Binds
    @IntoMap
    @ViewModelKey(ExercisesViewModel::class)
    abstract fun exercisesViewModel(viewModel: ExercisesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ExerciseViewModel::class)
    abstract fun exerciseViewModel(viewModel: ExerciseViewModel): ViewModel
}
