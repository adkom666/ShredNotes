package com.adkom666.shrednotes.data.repository

import com.adkom666.shrednotes.data.db.ShredNotesDatabase
import com.adkom666.shrednotes.data.db.Transactor

class TestNoteRepositoryKeeper {

    val repository: NoteRepository
        get() = requireNotNull(_repository)

    private var _repository: NoteRepository? = null

    fun createRepository(db: ShredNotesDatabase) {
        val noteDao = db.noteDao()
        val transactor = Transactor(db)
        _repository = NoteRepositoryImpl(noteDao, transactor)
    }

    fun destroyRepository() {
        _repository = null
    }
}
