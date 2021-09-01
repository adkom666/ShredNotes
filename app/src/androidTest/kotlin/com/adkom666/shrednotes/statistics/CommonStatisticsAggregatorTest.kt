package com.adkom666.shrednotes.statistics

import com.adkom666.shrednotes.common.toId
import com.adkom666.shrednotes.data.db.ShredNotesDatabase
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.dao.NoteDao
import com.adkom666.shrednotes.data.db.entity.ExerciseEntity
import com.adkom666.shrednotes.data.db.entity.NoteEntity
import com.adkom666.shrednotes.di.component.DaggerTestStatisticsComponent
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
        val statistics = statisticsAggregator.aggregate()
        assertEquals(statistics.totalNotes, 0)
        assertEquals(statistics.totalExercises, 0)
        assertEquals(statistics.totalDays, 0)
        assertEquals(statistics.activeDays, 0)
        assertEquals(statistics.activeDaysShare, null)
    }

    fun testAggregate() = runBlocking {

        fillDatabase()

        val statistics = statisticsAggregator.aggregate()
        assertEquals(statistics.totalNotes, 3)
        assertEquals(statistics.totalExercises, 2)
        assertEquals(statistics.totalDays, 3)
        assertEquals(statistics.activeDays, 2)
        assertEquals(statistics.activeDaysShare, 2f / 3)
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
            timestamp = Days().epochMillis,
            exerciseId = exerciseEntity1.id,
            bpm = 666
        )
        noteDao.insert(noteEntity1)

        val noteEntity2 = NoteEntity(
            id = 2.toId(),
            timestamp = Days().yesterday.yesterday.epochMillis,
            exerciseId = exerciseEntity2.id,
            bpm = 256
        )
        noteDao.insert(noteEntity2)

        val noteEntity3 = NoteEntity(
            id = 3.toId(),
            timestamp = Days().epochMillis,
            exerciseId = exerciseEntity2.id,
            bpm = 512
        )
        noteDao.insert(noteEntity3)
    }
}
