package com.adkom666.shrednotes.di.module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.adkom666.shrednotes.di.viewmodel.ViewModelFactory
import com.adkom666.shrednotes.di.viewmodel.ViewModelKey
import com.adkom666.shrednotes.ui.gdrivedialog.GoogleDriveViewModel
import com.adkom666.shrednotes.ui.MainViewModel
import com.adkom666.shrednotes.ui.ask.AskViewModel
import com.adkom666.shrednotes.ui.exercises.ExerciseViewModel
import com.adkom666.shrednotes.ui.exercises.ExercisesViewModel
import com.adkom666.shrednotes.ui.notes.NoteViewModel
import com.adkom666.shrednotes.ui.notes.NotesViewModel
import com.adkom666.shrednotes.ui.statistics.CommonStatisticsViewModel
import com.adkom666.shrednotes.ui.statistics.RecordsViewModel
import com.adkom666.shrednotes.ui.statistics.StatisticsViewModel
import com.adkom666.shrednotes.ui.statistics.TrackingViewModel
import com.adkom666.shrednotes.ui.statistics.WeekdaysStatisticsViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.time.ExperimentalTime

@Suppress("unused", "UndocumentedPublicClass", "UndocumentedPublicFunction")
@ExperimentalCoroutinesApi
@ExperimentalTime
@Module(
    includes = [
        RepositoryModule::class,
        DataManagerModule::class,
        PreferencesModule::class,
        AskModule::class,
        StatisticsModule::class
    ]
)
abstract class ViewModelModule {

    @Binds
    abstract fun viewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun mainViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ExercisesViewModel::class)
    abstract fun exercisesViewModel(viewModel: ExercisesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ExerciseViewModel::class)
    abstract fun exerciseViewModel(viewModel: ExerciseViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NotesViewModel::class)
    abstract fun notesViewModel(viewModel: NotesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NoteViewModel::class)
    abstract fun noteViewModel(viewModel: NoteViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AskViewModel::class)
    abstract fun askViewModel(viewModel: AskViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(StatisticsViewModel::class)
    abstract fun statisticsViewModel(viewModel: StatisticsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CommonStatisticsViewModel::class)
    abstract fun commonStatisticsViewModel(viewModel: CommonStatisticsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(WeekdaysStatisticsViewModel::class)
    abstract fun weekdaysStatisticsViewModel(viewModel: WeekdaysStatisticsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RecordsViewModel::class)
    abstract fun recordsViewModel(viewModel: RecordsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TrackingViewModel::class)
    abstract fun trackingViewModel(viewModel: TrackingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GoogleDriveViewModel::class)
    abstract fun googleDriveViewModel(viewModel: GoogleDriveViewModel): ViewModel
}
