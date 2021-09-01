package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.common.toId
import com.adkom666.shrednotes.data.db.ShredNotesDatabase
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.dao.NoteDao
import com.adkom666.shrednotes.data.db.entity.ExerciseEntity
import com.adkom666.shrednotes.data.db.entity.NoteEntity
import com.adkom666.shrednotes.di.component.DaggerTestStatisticsComponent
import java.util.Date
import javax.inject.Inject
import junit.framework.TestCase
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.runBlocking

@ExperimentalTime
class RecordsAggregatorTest : TestCase() {

    private companion object {
        private const val EXERCISE_NAME_1 = "exercise 1"
        private const val EXERCISE_NAME_2 = "exercise 2"
        private const val RECORDS_LIMIT = 6666
        private val BPM = arrayOf(666, 512, 256)
    }

    @Inject
    lateinit var database: ShredNotesDatabase

    @Inject
    lateinit var exerciseDao: ExerciseDao

    @Inject
    lateinit var noteDao: NoteDao

    @Inject
    lateinit var recordsAggregator: RecordsAggregator

    public override fun setUp() {
        super.setUp()
        DaggerTestStatisticsComponent.create().inject(this)
        fillDatabase()
    }

    public override fun tearDown() {
        super.tearDown()
        database.close()
    }

    fun testAggregateBpmRecords() = runBlocking {
        val records = recordsAggregator.aggregateBpmRecords(limit = RECORDS_LIMIT)
        assertEquals(records.topNotes.size, 3)
        assertEquals(records.topNotes[0].bpm, BPM[0])
        assertEquals(records.topNotes[1].bpm, BPM[1])
        assertEquals(records.topNotes[2].bpm, BPM[2])
    }

    fun testAggregateNoteCountRecords() = runBlocking {
        val records = recordsAggregator.aggregateNoteCountRecords(limit = RECORDS_LIMIT)
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

    private fun fillDatabase() {
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
            bpm = BPM[0]
        )
        noteDao.insert(noteEntity1)
        val noteEntity2 = NoteEntity(
            id = 2.toId(),
            timestamp = Date().time,
            exerciseId = exerciseEntity2.id,
            bpm = BPM[2]
        )
        noteDao.insert(noteEntity2)
        val noteEntity3 = NoteEntity(
            id = 3.toId(),
            timestamp = Date().time,
            exerciseId = exerciseEntity2.id,
            bpm = BPM[1]
        )
        noteDao.insert(noteEntity3)
    }
}
