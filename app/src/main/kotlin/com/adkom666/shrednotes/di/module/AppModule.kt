package com.adkom666.shrednotes.di.module

import com.adkom666.shrednotes.ui.MainActivity
import com.adkom666.shrednotes.ui.ask.AskFragment
import com.adkom666.shrednotes.ui.exercises.ExerciseActivity
import com.adkom666.shrednotes.ui.exercises.ExercisesFragment
import com.adkom666.shrednotes.ui.notes.NoteActivity
import com.adkom666.shrednotes.ui.notes.NotesFragment
import com.adkom666.shrednotes.ui.statistics.CommonStatisticsActivity
import com.adkom666.shrednotes.ui.statistics.RecordsActivity
import com.adkom666.shrednotes.ui.statistics.StatisticsFragment
import com.adkom666.shrednotes.ui.statistics.WeekdaysStatisticsActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector
import kotlinx.coroutines.ExperimentalCoroutinesApi

@Suppress("unused", "UndocumentedPublicClass", "UndocumentedPublicFunction")
@ExperimentalCoroutinesApi
@Module(includes = [ViewModelModule::class])
abstract class AppModule {

    @ContributesAndroidInjector
    abstract fun mainActivity(): MainActivity

    @ContributesAndroidInjector
    abstract fun exercisesFragment(): ExercisesFragment

    @ContributesAndroidInjector
    abstract fun exerciseActivity(): ExerciseActivity

    @ContributesAndroidInjector
    abstract fun notesFragment(): NotesFragment

    @ContributesAndroidInjector
    abstract fun noteActivity(): NoteActivity

    @ContributesAndroidInjector
    abstract fun askFragment(): AskFragment

    @ContributesAndroidInjector
    abstract fun statisticsFragment(): StatisticsFragment

    @ContributesAndroidInjector
    abstract fun commonStatisticsActivity(): CommonStatisticsActivity

    @ContributesAndroidInjector
    abstract fun weekdaysStatisticsActivity(): WeekdaysStatisticsActivity

    @ContributesAndroidInjector
    abstract fun recordsActivity(): RecordsActivity
}
