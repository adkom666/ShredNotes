package com.adkom666.shrednotes.data.repository

import com.adkom666.shrednotes.data.db.ShredNotesDatabase
import com.adkom666.shrednotes.data.db.Transactor

class TestExerciseRepositoryKeeper {

    val repository: ExerciseRepository
        get() = requireNotNull(_repository)

    private var _repository: ExerciseRepository? = null

    fun createRepository(db: ShredNotesDatabase) {
        val exerciseDao = db.exerciseDao()
        val transactor = Transactor(db)
        _repository = ExerciseRepositoryImpl(exerciseDao, transactor)
    }

    fun destroyRepository() {
        _repository = null
    }
}
