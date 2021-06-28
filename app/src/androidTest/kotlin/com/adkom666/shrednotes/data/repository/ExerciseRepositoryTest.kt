package com.adkom666.shrednotes.data.repository

import com.adkom666.shrednotes.data.db.StoreExerciseTestHelper
import com.adkom666.shrednotes.data.db.TestDbKeeper
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking

class ExerciseRepositoryTest : TestCase() {

    private val dbKeeper: TestDbKeeper = TestDbKeeper()
    private val repositoryKeeper: TestExerciseRepositoryKeeper = TestExerciseRepositoryKeeper()

    private val repository: ExerciseRepository
        get() = repositoryKeeper.repository

    public override fun setUp() {
        super.setUp()
        dbKeeper.createDb()
        repositoryKeeper.createRepository(dbKeeper.db)

        val db = dbKeeper.db
        val exerciseDao = db.exerciseDao()
        StoreExerciseTestHelper.insertExercises(exerciseDao)
    }

    public override fun tearDown() {
        super.tearDown()
        repositoryKeeper.destroyRepository()
        dbKeeper.destroyDb()
    }

    fun testCountWithoutParameters() = runBlocking {
        val count = repository.countSuspending(
            subname = null
        )
        assertEquals(StoreExerciseTestHelper.EXERCISE_COUNT, count)
    }

    fun testCountBySubname() = runBlocking {
        val exerciseDao = dbKeeper.db.exerciseDao()
        val exerciseList = exerciseDao.list(StoreExerciseTestHelper.EXERCISE_COUNT, 0)
        val countBySubname = exerciseList.count {
            it.name.contains(StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME, true)
        }
        val count = repository.countSuspending(
            subname = StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME
        )
        assertEquals(countBySubname, count)
    }

    fun testListWithoutParameters() = runBlocking {
        val list = repository.list(
            size = StoreExerciseTestHelper.EXERCISE_COUNT,
            startPosition = 0,
            subname = null
        )
        assertEquals(StoreExerciseTestHelper.EXERCISE_COUNT, list.size)
    }

    fun testListBySubname() = runBlocking {
        val exerciseDao = dbKeeper.db.exerciseDao()
        val exerciseList = exerciseDao.list(StoreExerciseTestHelper.EXERCISE_COUNT, 0)
        val countBySubname = exerciseList.count {
            it.name.contains(StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME, true)
        }
        val list = repository.list(
            size = StoreExerciseTestHelper.EXERCISE_COUNT,
            startPosition = 0,
            subname = StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME
        )
        assertEquals(countBySubname, list.size)
    }

    fun testPageWithoutParameters() = runBlocking {
        val page = repository.page(
            size = StoreExerciseTestHelper.EXERCISE_COUNT,
            requestedStartPosition = 0,
            subname = null
        )
        assertEquals(StoreExerciseTestHelper.EXERCISE_COUNT, page.items.size)
        assertEquals(0, page.offset)
    }

    fun testPageWithTranscendentalStartPosition() = runBlocking {
        val pageSize = StoreExerciseTestHelper.EXERCISE_COUNT / 3
        val page = repository.page(
            size = pageSize,
            requestedStartPosition = StoreExerciseTestHelper.EXERCISE_COUNT * 666,
            subname = null
        )
        assertEquals(pageSize, page.items.size)
        assertEquals(StoreExerciseTestHelper.EXERCISE_COUNT - pageSize, page.offset)
    }

    fun testDeletionByIdsAndSubname() = runBlocking {
        val groupSize = StoreExerciseTestHelper.EXERCISE_COUNT / 3
        val exerciseDao = dbKeeper.db.exerciseDao()
        val exerciseList = exerciseDao.list(groupSize, 0)
        val ids = exerciseList.map { it.id }

        var deletedExerciseCount = repository.deleteSuspending(
            ids = ids,
            subname = StoreExerciseTestHelper.EXERCISE_NAME_NOT_CONSISTS_IT
        )
        assertEquals(0, deletedExerciseCount)

        val countWithSubname = exerciseList.count {
            it.name.contains(StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME, true)
        }
        deletedExerciseCount = repository.deleteSuspending(
            ids = ids,
            subname = StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME
        )
        assertEquals(countWithSubname, deletedExerciseCount)
    }
    
    fun testDeletionOtherByIdsAndSubname() = runBlocking {
        val groupSize = StoreExerciseTestHelper.EXERCISE_COUNT / 3
        val exerciseDao = dbKeeper.db.exerciseDao()
        val exerciseList = exerciseDao.list(groupSize, 0)
        val ids = exerciseList.map { it.id }

        var deletedExerciseCount = repository.deleteOtherSuspending(
            ids = ids,
            subname = StoreExerciseTestHelper.EXERCISE_NAME_NOT_CONSISTS_IT
        )
        assertEquals(0, deletedExerciseCount)

        val otherExerciseList = exerciseDao.list(
            size = StoreExerciseTestHelper.EXERCISE_COUNT - groupSize,
            offset = groupSize
        )
        val countWithSubname = otherExerciseList.count {
            it.name.contains(StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME, true)
        }
        deletedExerciseCount = repository.deleteOtherSuspending(
            ids = ids,
            subname = StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME
        )
        assertEquals(countWithSubname, deletedExerciseCount)
    }
}
