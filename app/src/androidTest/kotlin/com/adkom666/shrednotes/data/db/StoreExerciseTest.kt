package com.adkom666.shrednotes.data.db

import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.startsWith
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

class StoreExerciseTest {

    private val exerciseDao: ExerciseDao
        get() = _dbKeeper.db.exerciseDao()

    private val _dbKeeper = TestDbKeeper()

    @Before
    fun prepareDb() {
        _dbKeeper.createDb()
        StoreExerciseTestHelper.insertExercises(exerciseDao)
    }

    @After
    fun destroyDb() {
        _dbKeeper.destroyDb()
    }

    @Test
    fun testCount() {
        val exerciseCount = exerciseDao.count()
        assertEquals(StoreExerciseTestHelper.EXERCISE_COUNT, exerciseCount)
    }

    @Test
    fun testNames() {
        val exerciseList = exerciseDao.entities(StoreExerciseTestHelper.EXERCISE_COUNT, 0)
        exerciseList.forEach { exerciseEntity ->
            assertThat(
                exerciseEntity.name,
                startsWith(StoreExerciseTestHelper.EXERCISE_NAME_PREFIX)
            )
        }
    }

    @Test
    fun testUpdate() {
        val updatedExerciseNamePrefix = "Updated Test Exercise "
        val exerciseList = exerciseDao.entities(StoreExerciseTestHelper.EXERCISE_COUNT, 0)
        exerciseList.forEach { exerciseEntity ->
            val updatedExerciseName = exerciseEntity.name.replace(
                StoreExerciseTestHelper.EXERCISE_NAME_PREFIX,
                updatedExerciseNamePrefix
            )
            val updatedExerciseEntity = exerciseEntity.copy(name = updatedExerciseName)
            exerciseDao.update(updatedExerciseEntity)
        }
        val updatedExerciseList = exerciseDao.entities(StoreExerciseTestHelper.EXERCISE_COUNT, 0)
        updatedExerciseList.forEach { exerciseEntity ->
            assertThat(exerciseEntity.name, startsWith(updatedExerciseNamePrefix))
        }
    }

    @Test
    fun testDeletion() {
        val exerciseList = exerciseDao.entities(1, 0)
        val exerciseEntity = exerciseList[0]
        exerciseDao.delete(exerciseEntity)
        val exerciseCount = exerciseDao.count()
        assertEquals(StoreExerciseTestHelper.EXERCISE_COUNT - 1, exerciseCount)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun testDeletionByIdsAndSubname() = runBlocking {
        val sublistSize = StoreExerciseTestHelper.EXERCISE_COUNT / 3
        val exerciseList = exerciseDao.entities(sublistSize, 0)
        val ids = exerciseList.map { it.id }
        var deletedExerciseCount = exerciseDao.deleteByIdsAndSubnameSuspending(
            ids,
            StoreExerciseTestHelper.EXERCISE_NAME_NOT_CONSISTS_IT
        )
        assertEquals(0, deletedExerciseCount)
        val countWithSubname = StoreExerciseTestHelper.countBySubname(
            exerciseList,
            StoreExerciseTestHelper.EXERCISE_SUBNAME
        )
        deletedExerciseCount = exerciseDao.deleteByIdsAndSubnameSuspending(
            ids,
            StoreExerciseTestHelper.EXERCISE_SUBNAME
        )
        assertEquals(countWithSubname, deletedExerciseCount)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun testDeletionOtherByIdsAndSubname() = runBlocking {
        val sublistSize = StoreExerciseTestHelper.EXERCISE_COUNT / 3
        val exerciseList = exerciseDao.entities(
            size = sublistSize,
            offset = 0
        )
        val ids = exerciseList.map { it.id }
        var deletedExerciseCount = exerciseDao.deleteOtherByIdsAndSubnameSuspending(
            ids,
            StoreExerciseTestHelper.EXERCISE_NAME_NOT_CONSISTS_IT
        )
        assertEquals(0, deletedExerciseCount)
        val otherExerciseList = exerciseDao.entities(
            size = StoreExerciseTestHelper.EXERCISE_COUNT - sublistSize,
            offset = sublistSize
        )
        val countWithSubname = StoreExerciseTestHelper.countBySubname(
            otherExerciseList,
            StoreExerciseTestHelper.EXERCISE_SUBNAME
        )
        deletedExerciseCount = exerciseDao.deleteOtherByIdsAndSubnameSuspending(
            ids,
            StoreExerciseTestHelper.EXERCISE_SUBNAME
        )
        assertEquals(countWithSubname, deletedExerciseCount)
    }

    @Test
    fun testPage() {
        val size = StoreExerciseTestHelper.EXERCISE_COUNT / 4
        val requestedOffset = StoreExerciseTestHelper.EXERCISE_COUNT / 2
        val page = exerciseDao.page(size, requestedOffset, null)
        assertEquals(size, page.items.size)
        assertEquals(requestedOffset, page.offset)
    }

    @Test
    fun testPageWithTranscendentalRequestedOffset() {
        val size = StoreExerciseTestHelper.EXERCISE_COUNT / 4
        val requestedOffset = StoreExerciseTestHelper.EXERCISE_COUNT * 2
        val page = exerciseDao.page(size, requestedOffset, null)
        assertEquals(size, page.items.size)
        assertEquals(StoreExerciseTestHelper.EXERCISE_COUNT - size, page.offset)
    }
}
