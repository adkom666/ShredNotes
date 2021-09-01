package com.adkom666.shrednotes.data.db

import android.database.sqlite.SQLiteConstraintException
import com.adkom666.shrednotes.common.NO_ID
import com.adkom666.shrednotes.common.toId
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.dao.NoteDao
import com.adkom666.shrednotes.data.db.entity.NoteEntity
import com.adkom666.shrednotes.di.component.DaggerTestDatabaseComponent
import com.adkom666.shrednotes.util.time.Minutes
import javax.inject.Inject
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking

class StoreNoteTest : TestCase() {

    @Inject
    lateinit var database: ShredNotesDatabase

    @Inject
    lateinit var exerciseDao: ExerciseDao

    @Inject
    lateinit var noteDao: NoteDao

    override fun setUp() {
        super.setUp()
        DaggerTestDatabaseComponent.create().inject(this)
        StoreExerciseTestHelper.insertExercises(exerciseDao)
        StoreNoteTestHelper.insertNotes(noteDao, exerciseDao)
    }

    override fun tearDown() {
        super.tearDown()
        database.close()
    }

    fun testCountAll() = runBlocking {
        val noteCount = noteDao.countAllSuspending()
        assertEquals(StoreNoteTestHelper.NOTE_COUNT, noteCount)
    }

    fun testCountByExerciseIds() = runBlocking {
        val groupSize = StoreExerciseTestHelper.EXERCISE_COUNT / 3
        val exerciseList = exerciseDao.list(groupSize, 0)
        val exerciseIds = exerciseList.map { it.id }
        val noteCount = noteDao.countByExerciseIdsSuspending(exerciseIds)
        val allNoteList = noteDao.listAllUnorderedSuspending()
        val noteCountReal = allNoteList.count {
            exerciseIds.contains(it.exerciseId)
        }
        assertEquals(noteCountReal, noteCount)
    }

    fun testCountOtherByExerciseIds() = runBlocking {
        val groupSize = StoreExerciseTestHelper.EXERCISE_COUNT / 3
        val exerciseList = exerciseDao.list(groupSize, 0)
        val exerciseIds = exerciseList.map { it.id }
        val noteCount = noteDao.countOtherByExerciseIdsSuspending(exerciseIds)
        val allNoteList = noteDao.listAllUnorderedSuspending()
        val noteCountReal = allNoteList.count {
            exerciseIds.contains(it.exerciseId).not()
        }
        assertEquals(noteCountReal, noteCount)
    }

    fun testInsertNoteWithExercise() {
        val noteWithExerciseEntity = NoteEntity(
            id = NO_ID,
            timestamp = Minutes().epochMillis,
            exerciseId = StoreNoteTestHelper.existentExerciseId(exerciseDao),
            bpm = 666
        )
        val noteId = noteDao.insert(noteWithExerciseEntity).toId()
        assertFalse(NO_ID == noteId)
    }

    fun testInsertNoteWithoutExercise() {
        val noteWithoutExerciseEntity = NoteEntity(
            id = NO_ID,
            timestamp = Minutes().epochMillis,
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

    fun testDeletionByIdsAndSubname() = runBlocking {
        val groupSize = StoreNoteTestHelper.NOTE_COUNT / 3
        val noteList = noteDao.list(groupSize, 0)
        val ids = noteList.map { it.noteId }

        var deletedNoteCount = noteDao.deleteByIdsAndExerciseSubnameSuspending(
            ids,
            StoreExerciseTestHelper.EXERCISE_NAME_NOT_CONSISTS_IT
        )
        assertEquals(0, deletedNoteCount)

        val countWithExerciseSubname = noteList.count {
            it.exerciseName
                ?.contains(StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME, true)
                ?: false
        }
        deletedNoteCount = noteDao.deleteByIdsAndExerciseSubnameSuspending(
            ids,
            StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME
        )
        assertEquals(countWithExerciseSubname, deletedNoteCount)
    }

    fun testDeletionOtherByIdsAndExerciseSubname() = runBlocking {
        val groupSize = StoreNoteTestHelper.NOTE_COUNT / 3
        val noteList = noteDao.list(groupSize, 0)
        val ids = noteList.map { it.noteId }

        var deletedNoteCount = noteDao.deleteOtherByIdsAndExerciseSubnameSuspending(
            ids,
            StoreExerciseTestHelper.EXERCISE_NAME_NOT_CONSISTS_IT
        )
        assertEquals(0, deletedNoteCount)

        val otherNoteList = noteDao.list(
            size = StoreNoteTestHelper.NOTE_COUNT - groupSize,
            offset = groupSize
        )
        val countWithExerciseSubname = otherNoteList.count {
            it.exerciseName
                ?.contains(StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME, true)
                ?: false
        }
        deletedNoteCount = noteDao.deleteOtherByIdsAndExerciseSubnameSuspending(
            ids,
            StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME
        )
        assertEquals(countWithExerciseSubname, deletedNoteCount)
    }
}
