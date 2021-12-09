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
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.time.ExperimentalTime

@ExperimentalTime
class TrackingAggregatorTest : TestCase() {

    private companion object {
        private const val EXERCISE_NAME_1 = "exercise 1"
        private const val EXERCISE_NAME_2 = "exercise 2"

        private val BPM = arrayOf(256, 666, 512)

        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1_000L
    }

    @Inject
    lateinit var database: ShredNotesDatabase

    @Inject
    lateinit var exerciseDao: ExerciseDao

    @Inject
    lateinit var noteDao: NoteDao

    @Inject
    lateinit var trackingAggregator: TrackingAggregator

    public override fun setUp() {
        super.setUp()
        DaggerTestStatisticsComponent.create().inject(this)
        fillDatabase()
    }

    public override fun tearDown() {
        super.tearDown()
        database.close()
    }

    fun testAggregateMaxBpmTrackingForInfiniteDateRange() = runBlocking {
        val statistics = trackingAggregator.aggregateMaxBpmTracking(
            dateRange = INFINITE_DATE_RANGE,
            exerciseId = null
        )
        assertEquals(2, statistics.points.size)
        assertEquals(BPM[0], statistics.points[0].maxBpm)
        assertEquals(BPM[1], statistics.points[1].maxBpm)
    }

    fun testAggregateMaxBpmTrackingForCustomDateRange() = runBlocking {
        val dateRange = DateRange(Days().yesterday, Days().tomorrow)
        val statistics = trackingAggregator.aggregateMaxBpmTracking(
            dateRange = dateRange,
            exerciseId = null
        )
        assertEquals(1, statistics.points.size)
        assertEquals(BPM[1], statistics.points[0].maxBpm)
    }

    fun testAggregateMaxBpmTrackingForPredefinedExercise() = runBlocking {
        val statistics = trackingAggregator.aggregateMaxBpmTracking(
            dateRange = INFINITE_DATE_RANGE,
            exerciseId = 1.toId()
        )
        assertEquals(1, statistics.points.size)
        assertEquals(BPM[1], statistics.points[0].maxBpm)
    }

    fun testAggregateNoteCountTrackingForInfiniteDateRange() = runBlocking {
        val statistics = trackingAggregator.aggregateNoteCountTracking(
            dateRange = INFINITE_DATE_RANGE,
            exerciseId = null
        )
        assertEquals(2, statistics.points.size)
        assertEquals(1, statistics.points[0].noteCount)
        assertEquals(2, statistics.points[1].noteCount)
    }

    fun testAggregateNoteCountTrackingForCustomDateRange() = runBlocking {
        val dateRange = DateRange(Days().yesterday, Days().tomorrow)
        val statistics = trackingAggregator.aggregateNoteCountTracking(
            dateRange = dateRange,
            exerciseId = null
        )
        assertEquals(1, statistics.points.size)
        assertEquals(2, statistics.points[0].noteCount)
    }

    fun testAggregateNoteCountTrackingForPredefinedExercise() = runBlocking {
        val statistics = trackingAggregator.aggregateNoteCountTracking(
            dateRange = INFINITE_DATE_RANGE,
            exerciseId = 1.toId()
        )
        assertEquals(1, statistics.points.size)
        assertEquals(2, statistics.points[0].noteCount)
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
            timestamp = System.currentTimeMillis(),
            exerciseId = exerciseEntity1.id,
            bpm = BPM[1]
        )
        noteDao.insert(noteEntity1)

        val noteEntity2 = NoteEntity(
            id = 2.toId(),
            timestamp = System.currentTimeMillis() - 2 * MILLIS_PER_DAY,
            exerciseId = exerciseEntity2.id,
            bpm = BPM[0]
        )
        noteDao.insert(noteEntity2)

        val noteEntity3 = NoteEntity(
            id = 3.toId(),
            timestamp = System.currentTimeMillis(),
            exerciseId = exerciseEntity1.id,
            bpm = BPM[2]
        )
        noteDao.insert(noteEntity3)
    }
}
