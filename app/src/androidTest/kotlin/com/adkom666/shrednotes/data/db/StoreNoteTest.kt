package com.adkom666.shrednotes.data.db

import android.database.sqlite.SQLiteConstraintException
import com.adkom666.shrednotes.common.NO_ID
import com.adkom666.shrednotes.common.toId
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.dao.NoteDao
import com.adkom666.shrednotes.data.db.entity.NoteEntity
import junit.framework.TestCase
import org.junit.Test
import java.util.Date

class StoreNoteTest : TestCase() {

    private val exerciseDao: ExerciseDao
        get() = _dbKeeper.db.exerciseDao()

    private val noteDao: NoteDao
        get() = _dbKeeper.db.noteDao()

    private val _dbKeeper = TestDbKeeper()

    override fun setUp() {
        super.setUp()
        _dbKeeper.createDb()
        StoreExerciseTestHelper.insertExercises(exerciseDao)
        StoreNoteTestHelper.insertNotes(noteDao, exerciseDao)
    }

    override fun tearDown() {
        super.tearDown()
        _dbKeeper.destroyDb()
    }

    @Test
    fun testCount() {
        val noteCount = noteDao.countAll()
        assertEquals(StoreNoteTestHelper.NOTE_COUNT, noteCount)
    }

    @Test
    fun testInsertNoteWithExercise() {
        val noteWithExerciseEntity = NoteEntity(
            id = NO_ID,
            dateTime = Date(),
            exerciseId = StoreNoteTestHelper.existentExerciseId(exerciseDao),
            bpm = 666
        )
        val noteId = noteDao.insert(noteWithExerciseEntity).toId()
        assertFalse(NO_ID == noteId)
    }

    @Test
    fun testInsertNoteWithoutExercise() {
        val noteWithoutExerciseEntity = NoteEntity(
            id = NO_ID,
            dateTime = Date(),
            exerciseId = StoreNoteTestHelper.nonExistentExerciseId(exerciseDao),
            bpm = 666
        )
        try {
            noteDao.insert(noteWithoutExerciseEntity)
        } catch (e: SQLiteConstraintException) {
            assertTrue(true)
            return
        }
        assertFalse(true)
    }
}
