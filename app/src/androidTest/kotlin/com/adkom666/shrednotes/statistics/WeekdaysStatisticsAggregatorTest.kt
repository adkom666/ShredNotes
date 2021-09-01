package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.data.db.ShredNotesDatabase
import com.adkom666.shrednotes.data.db.StoreExerciseTestHelper
import com.adkom666.shrednotes.data.db.StoreNoteTestHelper
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.dao.NoteDao
import com.adkom666.shrednotes.data.db.entity.NoteEntity
import com.adkom666.shrednotes.di.component.DaggerTestStatisticsComponent
import com.adkom666.shrednotes.util.time.Days
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import junit.framework.TestCase
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.runBlocking
import timber.log.Timber

@ExperimentalTime
class WeekdaysStatisticsAggregatorTest : TestCase() {

    private companion object {
        private val ALL_WEEKDAYS = Weekday.values()
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
        StoreExerciseTestHelper.insertExercises(exerciseDao)
        StoreNoteTestHelper.insertNotes(noteDao, exerciseDao)
    }

    override fun tearDown() {
        super.tearDown()
        database.close()
    }

    fun testAggregateAverageAmongMaxBpm() = runBlocking {
        val averageAmongMaxBpm = aggregateAverageQuantity { noteList ->
            noteList.map { it.bpm }.maxOrNull()
        }

        val statistics = statisticsAggregator.aggregateAverageAmongMaxBpm()
        Timber.d("statistics.valueMap=${statistics.valueMap}")

        ALL_WEEKDAYS.forEach { weekday ->
            assertEquals(statistics.valueMap[weekday], averageAmongMaxBpm[weekday])
        }
    }

    fun testAggregateAverageNoteCount() = runBlocking {
        val averageNoteCount = aggregateAverageQuantity { noteList ->
            noteList.count()
        }

        val statistics = statisticsAggregator.aggregateAverageNoteCount()
        Timber.d("statistics.valueMap=${statistics.valueMap}")

        ALL_WEEKDAYS.forEach { weekday ->
            assertEquals(statistics.valueMap[weekday], averageNoteCount[weekday])
        }
    }

    private suspend fun aggregateAverageQuantity(
        calcQuantity: (List<NoteEntity>) -> Int?
    ): Map<Weekday, Float> {
        val noteEntityToDaysMap = noteEntityToDaysMap()
        Timber.d("noteEntityToDaysMap=$noteEntityToDaysMap")

        val weekdayToQuantityListMap = weekdayToEmptyIntListMap()
        val calendar = Calendar.getInstance()
        noteEntityToDaysMap.keys.forEach { days ->
            val weekday = days.date.toWeekday(calendar)
            noteEntityToDaysMap[days]?.let { noteEntityList ->
                calcQuantity(noteEntityList)?.let { quantity ->
                    weekdayToQuantityListMap[weekday]?.add(quantity)
                }
            }
        }

        val averageQuantity = mutableMapOf<Weekday, Float>()
        ALL_WEEKDAYS.forEach { weekday ->
            val quantityList = weekdayToQuantityListMap[weekday]
            if (quantityList.isNullOrEmpty().not()) {
                quantityList?.average()?.let {
                    averageQuantity[weekday] = it.toFloat()
                }
            }
            Timber.d("weekday=$weekday, quantityList=$quantityList")
            Timber.d("averageQuantity[$weekday]=${averageQuantity[weekday]}")
        }
        return averageQuantity
    }

    private suspend fun noteEntityToDaysMap(): Map<Days, List<NoteEntity>> {
        val noteEntities = noteDao.listAllUnorderedSuspending()
        val noteEntityToDaysMap = mutableMapOf<Days, MutableList<NoteEntity>>()
        noteEntities.forEach { note ->
            val days = Days(note.timestamp)
            val noteEntityList = noteEntityToDaysMap[days]
                ?: mutableListOf<NoteEntity>()
                    .also { noteEntityToDaysMap[days] = it }
            noteEntityList.add(note)
        }
        return noteEntityToDaysMap
    }

    private fun weekdayToEmptyIntListMap(): Map<Weekday, MutableList<Int>> {
        val weekdayToIntListMap = mutableMapOf<Weekday, MutableList<Int>>()
        ALL_WEEKDAYS.forEach { weekday ->
            weekdayToIntListMap[weekday] = mutableListOf()
        }
        return weekdayToIntListMap
    }

    private fun Date.toWeekday(calendar: Calendar): Weekday {
        calendar.time = this
        val weekdayNumber = calendar.get(Calendar.DAY_OF_WEEK)
        return ALL_WEEKDAYS[weekdayNumber - 1]
    }
}
