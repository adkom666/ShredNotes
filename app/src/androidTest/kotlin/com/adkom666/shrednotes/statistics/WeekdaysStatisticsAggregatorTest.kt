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
import com.adkom666.shrednotes.util.Weekday
import com.adkom666.shrednotes.util.time.Days
import java.util.Calendar
import javax.inject.Inject
import junit.framework.TestCase
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.runBlocking
import timber.log.Timber

@ExperimentalTime
class WeekdaysStatisticsAggregatorTest : TestCase() {

    private companion object {
        private const val EXERCISE_NAME_1 = "exercise 1"
        private const val EXERCISE_NAME_2 = "exercise 2"
        private val BPM = arrayOf(256, 666, 512, 300)
    }

    @Inject
    lateinit var database: ShredNotesDatabase

    @Inject
    lateinit var exerciseDao: ExerciseDao

    @Inject
    lateinit var noteDao: NoteDao

    @Inject
    lateinit var statisticsAggregator: WeekdaysStatisticsAggregator

    override fun setUp() {
        super.setUp()
        DaggerTestStatisticsComponent.create().inject(this)
        fillDatabase()
    }

    override fun tearDown() {
        super.tearDown()
        database.close()
    }

    fun testAggregateAverageAmongMaxBpmForInfiniteDateRange() = runBlocking {
        val statistics = statisticsAggregator.aggregateAverageAmongMaxBpm(INFINITE_DATE_RANGE)
        Timber.d("statistics.valueMap=${statistics.valueMap}")
        assertEquals(null, statistics.valueMap[Weekday.SUNDAY])
        assertEquals(BPM[0].toFloat(), statistics.valueMap[Weekday.MONDAY])
        assertEquals(null, statistics.valueMap[Weekday.TUESDAY])
        assertEquals((BPM[1] + BPM[3]).toFloat() / 2, statistics.valueMap[Weekday.WEDNESDAY])
        assertEquals(null, statistics.valueMap[Weekday.THURSDAY])
        assertEquals(null, statistics.valueMap[Weekday.FRIDAY])
        assertEquals(null, statistics.valueMap[Weekday.SATURDAY])
    }

    fun testAggregateAverageAmongMaxBpmForCustomDateRange() = runBlocking {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, 2)
        val monday = Days(calendar.timeInMillis)
        calendar.set(Calendar.DAY_OF_WEEK, 4)
        val wednesday = Days(calendar.timeInMillis)
        val dateRange = DateRange(monday, wednesday)
        val statistics = statisticsAggregator.aggregateAverageAmongMaxBpm(dateRange)
        Timber.d("statistics.valueMap=${statistics.valueMap}")
        assertEquals(null, statistics.valueMap[Weekday.SUNDAY])
        assertEquals(BPM[0].toFloat(), statistics.valueMap[Weekday.MONDAY])
        assertEquals(null, statistics.valueMap[Weekday.TUESDAY])
        assertEquals(BPM[1].toFloat(), statistics.valueMap[Weekday.WEDNESDAY])
        assertEquals(null, statistics.valueMap[Weekday.THURSDAY])
        assertEquals(null, statistics.valueMap[Weekday.FRIDAY])
        assertEquals(null, statistics.valueMap[Weekday.SATURDAY])
    }

    fun testAggregateAverageNoteCountForInfiniteDateRange() = runBlocking {
        val statistics = statisticsAggregator.aggregateAverageNoteCount(INFINITE_DATE_RANGE)
        Timber.d("statistics.valueMap=${statistics.valueMap}")
        assertEquals(null, statistics.valueMap[Weekday.SUNDAY])
        assertEquals(1f, statistics.valueMap[Weekday.MONDAY])
        assertEquals(null, statistics.valueMap[Weekday.TUESDAY])
        assertEquals((2f + 1f) / 2, statistics.valueMap[Weekday.WEDNESDAY])
        assertEquals(null, statistics.valueMap[Weekday.THURSDAY])
        assertEquals(null, statistics.valueMap[Weekday.FRIDAY])
        assertEquals(null, statistics.valueMap[Weekday.SATURDAY])
    }

    fun testAggregateAverageNoteCountForCustomDateRange() = runBlocking {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, 2)
        val monday = Days(calendar.timeInMillis)
        calendar.set(Calendar.DAY_OF_WEEK, 4)
        val wednesday = Days(calendar.timeInMillis)
        val dateRange = DateRange(monday, wednesday)
        val statistics = statisticsAggregator.aggregateAverageNoteCount(dateRange)
        Timber.d("statistics.valueMap=${statistics.valueMap}")
        assertEquals(null, statistics.valueMap[Weekday.SUNDAY])
        assertEquals(1f, statistics.valueMap[Weekday.MONDAY])
        assertEquals(null, statistics.valueMap[Weekday.TUESDAY])
        assertEquals(2f, statistics.valueMap[Weekday.WEDNESDAY])
        assertEquals(null, statistics.valueMap[Weekday.THURSDAY])
        assertEquals(null, statistics.valueMap[Weekday.FRIDAY])
        assertEquals(null, statistics.valueMap[Weekday.SATURDAY])
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

        val calendar = Calendar.getInstance()

        // Monday
        calendar.set(Calendar.DAY_OF_WEEK, 2)
        val noteEntity1 = NoteEntity(
            id = 1.toId(),
            timestamp = calendar.timeInMillis,
            exerciseId = exerciseEntity1.id,
            bpm = BPM[0]
        )
        noteDao.insert(noteEntity1)

        // Wednesday
        calendar.set(Calendar.DAY_OF_WEEK, 4)
        val noteEntity2 = NoteEntity(
            id = 2.toId(),
            timestamp = calendar.timeInMillis,
            exerciseId = exerciseEntity2.id,
            bpm = BPM[1]
        )
        noteDao.insert(noteEntity2)

        val noteEntity3 = NoteEntity(
            id = 3.toId(),
            timestamp = calendar.timeInMillis,
            exerciseId = exerciseEntity2.id,
            bpm = BPM[2]
        )
        noteDao.insert(noteEntity3)

        calendar.set(Calendar.YEAR, 1992)
        calendar.set(Calendar.DAY_OF_WEEK, 4)
        val noteEntity4 = NoteEntity(
            id = 4.toId(),
            timestamp = calendar.timeInMillis,
            exerciseId = exerciseEntity2.id,
            bpm = BPM[3]
        )
        noteDao.insert(noteEntity4)
    }
}
