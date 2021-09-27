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
class CommonStatisticsAggregatorTest : TestCase() {

    private companion object {
        private const val EXERCISE_NAME_1 = "exercise 1"
        private const val EXERCISE_NAME_2 = "exercise 2"
        private const val EXERCISE_NAME_3 = "exercise 3"
    }

    @Inject
    lateinit var database: ShredNotesDatabase

    @Inject
    lateinit var exerciseDao: ExerciseDao

    @Inject
    lateinit var noteDao: NoteDao

    @Inject
    lateinit var statisticsAggregator: CommonStatisticsAggregator

    public override fun setUp() {
        super.setUp()
        DaggerTestStatisticsComponent.create().inject(this)
    }

    public override fun tearDown() {
        super.tearDown()
        database.close()
    }

    fun testAggregateFromEmptySource() = runBlocking {
        val statistics = statisticsAggregator.aggregate(INFINITE_DATE_RANGE)
        assertEquals(0, statistics.notes)
        assertEquals(0, statistics.relatedExercises)
        assertEquals(0, statistics.days)
        assertEquals(0, statistics.activeDays)
        assertEquals(null, statistics.activeDaysShare)
        assertEquals(0, statistics.allNotes)
        assertEquals(0, statistics.allExercises)
    }

    fun testAggregateForInfiniteDateRange() = runBlocking {

        fillDatabase()

        val statistics = statisticsAggregator.aggregate(INFINITE_DATE_RANGE)
        assertEquals(3, statistics.notes)
        assertEquals(2, statistics.relatedExercises)
        assertEquals(3, statistics.days)
        assertEquals(2, statistics.activeDays)
        assertEquals(2f / 3, statistics.activeDaysShare)
        assertEquals(3, statistics.allNotes)
        assertEquals(3, statistics.allExercises)
    }

    fun testAggregateForCustomDateRange() = runBlocking {

        fillDatabase()

        val dateRange = DateRange(Days().yesterday, Days())
        val statistics = statisticsAggregator.aggregate(dateRange)
        assertEquals(2, statistics.notes)
        assertEquals(1, statistics.relatedExercises)
        assertEquals(2, statistics.days)
        assertEquals(1, statistics.activeDays)
        assertEquals(1f / 2, statistics.activeDaysShare)
        assertEquals(3, statistics.allNotes)
        assertEquals(3, statistics.allExercises)
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

        val exerciseEntity3 = ExerciseEntity(
            id = 3.toId(),
            name = EXERCISE_NAME_3
        )
        exerciseDao.insert(exerciseEntity3)

        val noteEntity1 = NoteEntity(
            id = 1.toId(),
            timestamp = Days().epochMillis,
            exerciseId = exerciseEntity1.id,
            bpm = 666
        )
        noteDao.insert(noteEntity1)

        val noteEntity2 = NoteEntity(
            id = 2.toId(),
            timestamp = Days().yesterday.epochMillis - 1L,
            exerciseId = exerciseEntity2.id,
            bpm = 256
        )
        noteDao.insert(noteEntity2)

        val noteEntity3 = NoteEntity(
            id = 3.toId(),
            timestamp = Days().epochMillis,
            exerciseId = exerciseEntity1.id,
            bpm = 512
        )
        noteDao.insert(noteEntity3)
    }
}
