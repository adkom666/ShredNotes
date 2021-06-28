package com.adkom666.shrednotes.data.db

import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.startsWith
import org.hamcrest.MatcherAssert.assertThat

class StoreExerciseTest : TestCase() {

    private val exerciseDao: ExerciseDao
        get() = _dbKeeper.db.exerciseDao()

    private val _dbKeeper: TestDbKeeper = TestDbKeeper()

    override fun setUp() {
        super.setUp()
        _dbKeeper.createDb()
        StoreExerciseTestHelper.insertExercises(exerciseDao)
    }

    override fun tearDown() {
        super.tearDown()
        _dbKeeper.destroyDb()
    }

    fun testCount() {
        val exerciseCount = exerciseDao.countAll()
        assertEquals(StoreExerciseTestHelper.EXERCISE_COUNT, exerciseCount)
    }

    fun testNames() {
        val exerciseList = exerciseDao.list(StoreExerciseTestHelper.EXERCISE_COUNT, 0)
        exerciseList.forEach { exerciseEntity ->
            assertThat(
                exerciseEntity.name,
                startsWith(StoreExerciseTestHelper.EXERCISE_NAME_PREFIX)
            )
        }
    }

    fun testUpdate() {
        val updatedExerciseNamePrefix = "Updated Test Exercise "
        val exerciseList = exerciseDao.list(StoreExerciseTestHelper.EXERCISE_COUNT, 0)
        exerciseList.forEach { exerciseEntity ->
            val updatedExerciseName = exerciseEntity.name.replace(
                StoreExerciseTestHelper.EXERCISE_NAME_PREFIX,
                updatedExerciseNamePrefix
            )
            val updatedExerciseEntity = exerciseEntity.copy(name = updatedExerciseName)
            exerciseDao.update(updatedExerciseEntity)
        }
        val updatedExerciseList = exerciseDao.list(StoreExerciseTestHelper.EXERCISE_COUNT, 0)
        updatedExerciseList.forEach { exerciseEntity ->
            assertThat(exerciseEntity.name, startsWith(updatedExerciseNamePrefix))
        }
    }

    fun testDeletion() {
        val exerciseList = exerciseDao.list(1, 0)
        val exerciseEntity = exerciseList[0]
        exerciseDao.delete(exerciseEntity)
        val exerciseCount = exerciseDao.countAll()
        assertEquals(StoreExerciseTestHelper.EXERCISE_COUNT - 1, exerciseCount)
    }

    fun testDeletionByIdsAndSubname() = runBlocking {
        val groupSize = StoreExerciseTestHelper.EXERCISE_COUNT / 3
        val exerciseList = exerciseDao.list(groupSize, 0)
        val ids = exerciseList.map { it.id }
        var deletedExerciseCount = exerciseDao.deleteByIdsAndSubnameSuspending(
            ids,
            StoreExerciseTestHelper.EXERCISE_NAME_NOT_CONSISTS_IT
        )
        assertEquals(0, deletedExerciseCount)
        val countWithSubname = exerciseList.count {
            it.name.contains(StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME, true)
        }
        deletedExerciseCount = exerciseDao.deleteByIdsAndSubnameSuspending(
            ids,
            StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME
        )
        assertEquals(countWithSubname, deletedExerciseCount)
    }

    fun testDeletionOtherByIdsAndSubname() = runBlocking {
        val groupSize = StoreExerciseTestHelper.EXERCISE_COUNT / 3
        val exerciseList = exerciseDao.list(
            size = groupSize,
            offset = 0
        )
        val ids = exerciseList.map { it.id }
        var deletedExerciseCount = exerciseDao.deleteOtherByIdsAndSubnameSuspending(
            ids,
            StoreExerciseTestHelper.EXERCISE_NAME_NOT_CONSISTS_IT
        )
        assertEquals(0, deletedExerciseCount)
        val otherExerciseList = exerciseDao.list(
            size = StoreExerciseTestHelper.EXERCISE_COUNT - groupSize,
            offset = groupSize
        )
        val countWithSubname = otherExerciseList.count {
            it.name.contains(StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME, true)
        }
        deletedExerciseCount = exerciseDao.deleteOtherByIdsAndSubnameSuspending(
            ids,
            StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME
        )
        assertEquals(countWithSubname, deletedExerciseCount)
    }
}
