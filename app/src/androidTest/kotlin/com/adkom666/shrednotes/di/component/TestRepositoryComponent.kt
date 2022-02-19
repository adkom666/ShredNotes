package com.adkom666.shrednotes.di.component

import com.adkom666.shrednotes.data.repository.ExerciseRepositoryTest
import com.adkom666.shrednotes.data.repository.NoteRepositoryTest
import com.adkom666.shrednotes.di.module.TestRepositoryModule
import dagger.Component
import javax.inject.Singleton

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@Singleton
@Component(modules = [TestRepositoryModule::class])
interface TestRepositoryComponent {
    fun inject(test: ExerciseRepositoryTest)
    fun inject(test: NoteRepositoryTest)
}
