package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.common.toId
import com.adkom666.shrednotes.data.db.TestDbKeeper
import com.adkom666.shrednotes.data.db.entity.ExerciseEntity
import com.adkom666.shrednotes.data.db.entity.NoteEntity
import com.adkom666.shrednotes.data.repository.TestNoteRepositoryKeeper
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import java.util.Date

class RecordsAggregatorTest : TestCase() {

    private companion object {
        private const val EXERCISE_NAME_1 = "exercise 1"
        private const val EXERCISE_NAME_2 = "exercise 2"
    }

    private val dbKeeper: TestDbKeeper = TestDbKeeper()
    private val repositoryKeeper: TestNoteRepositoryKeeper = TestNoteRepositoryKeeper()
    private val recordsAggregatorKeeper: TestRecordsAggregatorKeeper = TestRecordsAggregatorKeeper()

    private val recordsAggregator: RecordsAggregator
        get() = recordsAggregatorKeeper.recordsAggregator

    public override fun setUp() {
        super.setUp()

        dbKeeper.createDb()
        repositoryKeeper.createRepository(dbKeeper.db)
        recordsAggregatorKeeper.createRecordsAggregator(repositoryKeeper.repository)

        fillDb()
    }

    public override fun tearDown() {
        super.tearDown()

        recordsAggregatorKeeper.destroyRecordsAggregator()
        repositoryKeeper.destroyRepository()
        dbKeeper.destroyDb()
    }

    fun testAggregateBpmRecords() = runBlocking {
        val records = recordsAggregator.aggregateBpmRecords(limit = 6666)
        assertEquals(records.topNotes.size, 3)
        assertEquals(records.topNotes[0].bpm, 666)
        assertEquals(records.topNotes[1].bpm, 512)
        assertEquals(records.topNotes[2].bpm, 256)
    }

    fun testAggregateNoteCountRecords() = runBlocking {
        val records = recordsAggregator.aggregateNoteCountRecords(limit = 6666)
        assertEquals(records.topExerciseNames.size, 2)
        assertEquals(
            records.topExerciseNames[0],
            NoteCountRecords.Record(
                exerciseName = EXERCISE_NAME_2,
                noteCount = 2
            )
        )
        assertEquals(
            records.topExerciseNames[1],
            NoteCountRecords.Record(
                exerciseName = EXERCISE_NAME_1,
                noteCount = 1
            )
        )
    }

    private fun fillDb() {
        val db = dbKeeper.db
        val noteDao = db.noteDao()
        val exerciseDao = db.exerciseDao()
        val exerciseEntity1 = ExerciseEntity(
            id = 1.toId(),
            name = EXERCISE_NAME_1
        )
        exerciseDao.insert(exerciseEntity1)
        val exerciseEntity2 = ExerciseEntity(
            id = 2.toId(),
            name = EXERCISE_NAME_2
        )
        exerciseDao.insert(exerciseEntity2)
        val noteEntity1 = NoteEntity(
            id = 1.toId(),
            timestamp = Date().time,
            exerciseId = exerciseEntity1.id,
            bpm = 666
        )
        noteDao.insert(noteEntity1)
        val noteEntity2 = NoteEntity(
            id = 2.toId(),
            timestamp = Date().time,
            exerciseId = exerciseEntity2.id,
            bpm = 256
        )
        noteDao.insert(noteEntity2)
        val noteEntity3 = NoteEntity(
            id = 3.toId(),
            timestamp = Date().time,
            exerciseId = exerciseEntity2.id,
            bpm = 512
        )
        noteDao.insert(noteEntity3)
    }
}
