package com.adkom666.shrednotes.data.repository

import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.util.paging.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.Date

class NoteRepositoryImpl : NoteRepository {

    private companion object {
        private val NOTE_LIST = listOf(
            Note(
                id = 1,
                exercise = Exercise(name = "Ex. 666"),
                dateTime = Date(),
                bpm = 666
            ),
            Note(
                id = 2,
                exercise = Exercise(name = "Ex. 667"),
                dateTime = Date(),
                bpm = 667
            ),
        )
    }

    override val countFlow: Flow<Int>
        get() = flowOf(NOTE_LIST.size)

    override suspend fun countSuspending(subname: String?): Int {
        return NOTE_LIST.size
    }

    override fun page(
        size: Int,
        requestedStartPosition: Int,
        subname: String?
    ): Page<Note> {
        return Page(
            NOTE_LIST,
            requestedStartPosition
        )
    }

    override fun list(
        size: Int,
        startPosition: Int,
        subname: String?
    ): List<Note> {
        return emptyList()
    }

    override suspend fun saveSuspending(note: Note) {
    }

    override suspend fun deleteSuspending(ids: List<Long>, subname: String?): Int {
        return 0
    }

    override suspend fun deleteOtherSuspending(ids: List<Long>, subname: String?): Int {
        return 0
    }
}
