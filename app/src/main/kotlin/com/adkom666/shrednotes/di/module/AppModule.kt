package com.adkom666.shrednotes.di.module

import com.adkom666.shrednotes.ui.exercises.ExerciseActivity
import com.adkom666.shrednotes.ui.exercises.ExercisesFragment
import com.adkom666.shrednotes.ui.notes.NoteActivity
import com.adkom666.shrednotes.ui.notes.NotesFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector
import kotlinx.coroutines.ExperimentalCoroutinesApi

@Suppress("unused", "UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module
abstract class AppModule {

    @ExperimentalCoroutinesApi
    @ContributesAndroidInjector
    abstract fun exercisesFragment(): ExercisesFragment

    @ContributesAndroidInjector
    abstract fun exerciseActivity(): ExerciseActivity

    @ExperimentalCoroutinesApi
    @ContributesAndroidInjector
    abstract fun notesFragment(): NotesFragment

    @ContributesAndroidInjector
    abstract fun noteActivity(): NoteActivity
}
