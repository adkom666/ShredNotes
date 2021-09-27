package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.common.toId
import com.adkom666.shrednotes.data.db.ShredNotesDatabase
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.dao.NoteDao
import com.adkom666.shrednotes.data.db.entity.ExerciseEntity
import com.adkom666.shrednotes.data.db.entity.NoteEntity
import com.adkom666.shrednotes.di.component.DaggerTestStatisticsComponent
import com.adkom666.shrednotes.util.DateRange
import com.adkom666.shrednotes.util.INFINITE_DATE_RANGE
import com.adkom666.shrednotes.util.time.Days
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

    fun testAggregateBpmRecordsForInfiniteDateRange() = runBlocking {
        val records = recordsAggregator.aggregateBpmRecords(
            dateRange = INFINITE_DATE_RANGE,
            limit = RECORDS_LIMIT
        )
        assertEquals(2, records.topNotes.size)
        assertEquals(BPM[0], records.topNotes[0].bpm)
        assertEquals(BPM[1], records.topNotes[1].bpm)
    }

    fun testAggregateBpmRecordsForCustomDateRange() = runBlocking {
        val dateRange = DateRange(Days().yesterday, Days().tomorrow)
        val records = recordsAggregator.aggregateBpmRecords(
            dateRange = dateRange,
            limit = RECORDS_LIMIT
        )
        assertEquals(1, records.topNotes.size)
        assertEquals(BPM[2], records.topNotes[0].bpm)
    }

    fun testAggregateNoteCountRecordsForInfiniteDateRange() = runBlocking {
        val records = recordsAggregator.aggregateNoteCountRecords(
            dateRange = INFINITE_DATE_RANGE,
            limit = RECORDS_LIMIT
        )
        assertEquals(2, records.topExerciseNames.size)
        assertEquals(
            NoteCountRecords.Record(
                exerciseName = EXERCISE_NAME_2,
                noteCount = 2
            ),
            records.topExerciseNames[0]
        )
        assertEquals(
            NoteCountRecords.Record(
                exerciseName = EXERCISE_NAME_1,
                noteCount = 1
            ),
            records.topExerciseNames[1]
        )
    }

    fun testAggregateNoteCountRecordsForCustomDateRange() = runBlocking {
        val dateRange = DateRange(Days().yesterday, Days().tomorrow)
        val records = recordsAggregator.aggregateNoteCountRecords(
            dateRange = dateRange,
            limit = RECORDS_LIMIT
        )
        assertEquals(1, records.topExerciseNames.size)
        assertEquals(
            NoteCountRecords.Record(
                exerciseName = EXERCISE_NAME_2,
                noteCount = 1
            ),
            records.topExerciseNames[0]
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
            timestamp = Days().tomorrow.epochMillis,
            exerciseId = exerciseEntity1.id,
            bpm = BPM[0]
        )
        noteDao.insert(noteEntity1)

        val noteEntity2 = NoteEntity(
            id = 2.toId(),
            timestamp = Days().epochMillis,
            exerciseId = exerciseEntity2.id,
            bpm = BPM[2]
        )
        noteDao.insert(noteEntity2)

        val noteEntity3 = NoteEntity(
            id = 3.toId(),
            timestamp = Days().tomorrow.epochMillis,
            exerciseId = exerciseEntity2.id,
            bpm = BPM[1]
        )
        noteDao.insert(noteEntity3)
    }
}
