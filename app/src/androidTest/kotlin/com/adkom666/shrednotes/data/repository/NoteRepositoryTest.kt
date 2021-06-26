package com.adkom666.shrednotes.data.repository

import com.adkom666.shrednotes.data.db.StoreExerciseTestHelper
import com.adkom666.shrednotes.data.db.StoreNoteTestHelper
import com.adkom666.shrednotes.data.db.TestDbKeeper
import com.adkom666.shrednotes.data.model.NOTE_BPM_MAX
import com.adkom666.shrednotes.data.model.NOTE_BPM_MIN
import com.adkom666.shrednotes.data.model.NoteFilter
import com.adkom666.shrednotes.util.time.Days
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking

class NoteRepositoryTest : TestCase() {

    private val dbKeeper: TestDbKeeper = TestDbKeeper()
    private val repositoryKeeper: TestNoteRepositoryKeeper = TestNoteRepositoryKeeper()

    private val repository: NoteRepository
        get() = repositoryKeeper.repository

    public override fun setUp() {
        super.setUp()
        dbKeeper.createDb()
        repositoryKeeper.createRepository(dbKeeper.db)

        val db = dbKeeper.db
        val noteDao = db.noteDao()
        val exerciseDao = db.exerciseDao()
        StoreExerciseTestHelper.insertExercises(exerciseDao)
        StoreNoteTestHelper.insertNotes(noteDao, exerciseDao)
    }

    public override fun tearDown() {
        super.tearDown()
        repositoryKeeper.destroyRepository()
        dbKeeper.destroyDb()
    }

    fun testCountWithoutParameters() = runBlocking {
        val count = repository.countSuspending(
            exerciseSubname = null,
            filter = null
        )
        assertEquals(StoreNoteTestHelper.NOTE_COUNT, count)
    }

    fun testCountByExerciseSubname() = runBlocking {
        val noteDao = dbKeeper.db.noteDao()
        val noteList = noteDao.listPortion(StoreNoteTestHelper.NOTE_COUNT, 0)
        val countByExerciseSubname = noteList.count {
            it.exerciseName
                ?.contains(StoreExerciseTestHelper.EXERCISE_SUBNAME, true)
                ?: false
        }
        val count = repository.countSuspending(
            exerciseSubname = StoreExerciseTestHelper.EXERCISE_SUBNAME,
            filter = null
        )
        assertEquals(countByExerciseSubname, count)
    }

    fun testCountByDate() = runBlocking {
        val noteDao = dbKeeper.db.noteDao()
        val noteList = noteDao.listPortion(StoreNoteTestHelper.NOTE_COUNT, 0)

        val now = System.currentTimeMillis()
        val testDaysScatter = (0.666f * StoreNoteTestHelper.DAYS_SCATTER).toLong()
        val dateFromInclusive = Days(now - testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)
        val dateToExclusive = Days(now + testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)

        val countByDate = noteList.count { noteWithExercise ->
            val startMillis = dateFromInclusive.epochMillis
            val endMillis = dateToExclusive.epochMillis
            noteWithExercise.noteTimestamp in startMillis until endMillis
        }
        val filter = NoteFilter(
            dateFromInclusive = dateFromInclusive,
            dateToExclusive = dateToExclusive
        )
        val count = repository.countSuspending(
            exerciseSubname = null,
            filter = filter
        )
        assertEquals(countByDate, count)
    }

    fun testCountByBpm() = runBlocking {
        val noteDao = dbKeeper.db.noteDao()
        val noteList = noteDao.listPortion(StoreNoteTestHelper.NOTE_COUNT, 0)

        val bpmRange = NOTE_BPM_MAX - NOTE_BPM_MIN
        val bpmFromInclusive = NOTE_BPM_MIN + (0.2f * bpmRange).toInt()
        val bpmToInclusive = NOTE_BPM_MAX - (0.3f * bpmRange).toInt()

        val countByBpm = noteList.count {
            it.noteBpm in bpmFromInclusive..bpmToInclusive
        }
        val filter = NoteFilter(
            bpmFromInclusive = bpmFromInclusive,
            bpmToInclusive = bpmToInclusive
        )
        val count = repository.countSuspending(
            exerciseSubname = null,
            filter = filter
        )
        assertEquals(countByBpm, count)
    }

    fun testCountByExerciseSubnameDateAndBpm() = runBlocking {
        val noteDao = dbKeeper.db.noteDao()
        val noteList = noteDao.listPortion(StoreNoteTestHelper.NOTE_COUNT, 0)

        val now = System.currentTimeMillis()
        val testDaysScatter = (0.666f * StoreNoteTestHelper.DAYS_SCATTER).toLong()
        val dateFromInclusive = Days(now - testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)
        val dateToExclusive = Days(now + testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)

        val bpmRange = NOTE_BPM_MAX - NOTE_BPM_MIN
        val bpmFromInclusive = NOTE_BPM_MIN + (0.2f * bpmRange).toInt()
        val bpmToInclusive = NOTE_BPM_MAX - (0.3f * bpmRange).toInt()

        val countByExerciseSubnameDateAndBpm = noteList.count { noteWithExercise ->
            val startMillis = dateFromInclusive.epochMillis
            val endMillis = dateToExclusive.epochMillis
            (noteWithExercise.exerciseName
                ?.contains(StoreExerciseTestHelper.EXERCISE_SUBNAME, true)
                ?: false)
                    && noteWithExercise.noteTimestamp in startMillis until endMillis
                    && noteWithExercise.noteBpm in bpmFromInclusive..bpmToInclusive
        }
        val filter = NoteFilter(
            dateFromInclusive = dateFromInclusive,
            dateToExclusive = dateToExclusive,
            bpmFromInclusive = bpmFromInclusive,
            bpmToInclusive = bpmToInclusive
        )
        val count = repository.countSuspending(
            exerciseSubname = StoreExerciseTestHelper.EXERCISE_SUBNAME,
            filter = filter
        )
        assertEquals(countByExerciseSubnameDateAndBpm, count)
    }

    fun testListWithoutParameters() = runBlocking {
        val list = repository.list(
            size = StoreNoteTestHelper.NOTE_COUNT,
            startPosition = 0,
            exerciseSubname = null,
            filter = null
        )
        assertEquals(StoreNoteTestHelper.NOTE_COUNT, list.size)
    }

    fun testListByExerciseSubname() = runBlocking {
        val noteDao = dbKeeper.db.noteDao()
        val noteList = noteDao.listPortion(StoreNoteTestHelper.NOTE_COUNT, 0)
        val countByExerciseSubname = noteList.count {
            it.exerciseName
                ?.contains(StoreExerciseTestHelper.EXERCISE_SUBNAME, true)
                ?: false
        }
        val list = repository.list(
            size = StoreNoteTestHelper.NOTE_COUNT,
            startPosition = 0,
            exerciseSubname = StoreExerciseTestHelper.EXERCISE_SUBNAME,
            filter = null
        )
        assertEquals(countByExerciseSubname, list.size)
    }

    fun testPageWithoutParameters() = runBlocking {
        val page = repository.page(
            size = StoreNoteTestHelper.NOTE_COUNT,
            requestedStartPosition = 0,
            exerciseSubname = null,
            filter = null
        )
        assertEquals(StoreNoteTestHelper.NOTE_COUNT, page.items.size)
        assertEquals(0, page.offset)
    }

    fun testPageWithNonexistentStartPosition() = runBlocking {
        val pageSize = StoreNoteTestHelper.NOTE_COUNT / 3
        val page = repository.page(
            size = pageSize,
            requestedStartPosition = 666_666_666,
            exerciseSubname = null,
            filter = null
        )
        assertEquals(pageSize, page.items.size)
        assertEquals(StoreNoteTestHelper.NOTE_COUNT - pageSize, page.offset)
    }

    fun testDeletionByIdsAndExerciseSubname() = runBlocking {
        val groupSize = StoreNoteTestHelper.NOTE_COUNT / 3
        val noteDao = dbKeeper.db.noteDao()
        val noteList = noteDao.listPortion(groupSize, 0)
        val ids = noteList.map { it.noteId }

        var deletedNoteCount = repository.deleteSuspending(
            ids = ids,
            exerciseSubname = StoreExerciseTestHelper.EXERCISE_NAME_NOT_CONSISTS_IT,
            filter = null
        )
        assertEquals(0, deletedNoteCount)

        val countWithExerciseSubname = noteList.count {
            it.exerciseName
                ?.contains(StoreExerciseTestHelper.EXERCISE_SUBNAME, true)
                ?: false
        }
        deletedNoteCount = repository.deleteSuspending(
            ids = ids,
            exerciseSubname = StoreExerciseTestHelper.EXERCISE_SUBNAME,
            filter = null
        )
        assertEquals(countWithExerciseSubname, deletedNoteCount)
    }

    fun testDeletionByIdsAndDate() = runBlocking {
        val groupSize = StoreNoteTestHelper.NOTE_COUNT / 3
        val noteDao = dbKeeper.db.noteDao()
        val noteList = noteDao.listPortion(groupSize, 0)
        val ids = noteList.map { it.noteId }

        val now = System.currentTimeMillis()
        val testDaysScatter = (0.666f * StoreNoteTestHelper.DAYS_SCATTER).toLong()
        val dateFromInclusive = Days(now - testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)
        val dateToExclusive = Days(now + testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)

        val countByDate = noteList.count { noteWithExercise ->
            val startMillis = dateFromInclusive.epochMillis
            val endMillis = dateToExclusive.epochMillis
            noteWithExercise.noteTimestamp in startMillis until endMillis
        }
        val filter = NoteFilter(
            dateFromInclusive = dateFromInclusive,
            dateToExclusive = dateToExclusive
        )
        val deletedNoteCount = repository.deleteSuspending(
            ids = ids,
            exerciseSubname = null,
            filter = filter
        )
        assertEquals(countByDate, deletedNoteCount)
    }

    fun testDeletionByIdsAndBpm() = runBlocking {
        val groupSize = StoreNoteTestHelper.NOTE_COUNT / 3
        val noteDao = dbKeeper.db.noteDao()
        val noteList = noteDao.listPortion(groupSize, 0)
        val ids = noteList.map { it.noteId }

        val bpmRange = NOTE_BPM_MAX - NOTE_BPM_MIN
        val bpmFromInclusive = NOTE_BPM_MIN + (0.2f * bpmRange).toInt()
        val bpmToInclusive = NOTE_BPM_MAX - (0.3f * bpmRange).toInt()

        val countByBpm = noteList.count {
            it.noteBpm in bpmFromInclusive..bpmToInclusive
        }
        val filter = NoteFilter(
            bpmFromInclusive = bpmFromInclusive,
            bpmToInclusive = bpmToInclusive
        )
        val deletedNoteCount = repository.deleteSuspending(
            ids = ids,
            exerciseSubname = null,
            filter = filter
        )
        assertEquals(countByBpm, deletedNoteCount)
    }

    fun testDeletionByIdsDateAndBpm() = runBlocking {
        val groupSize = StoreNoteTestHelper.NOTE_COUNT / 3
        val noteDao = dbKeeper.db.noteDao()
        val noteList = noteDao.listPortion(groupSize, 0)
        val ids = noteList.map { it.noteId }

        val now = System.currentTimeMillis()
        val testDaysScatter = (0.666f * StoreNoteTestHelper.DAYS_SCATTER).toLong()
        val dateFromInclusive = Days(now - testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)
        val dateToExclusive = Days(now + testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)

        val bpmRange = NOTE_BPM_MAX - NOTE_BPM_MIN
        val bpmFromInclusive = NOTE_BPM_MIN + (0.2f * bpmRange).toInt()
        val bpmToInclusive = NOTE_BPM_MAX - (0.3f * bpmRange).toInt()

        val countByDateAndBpm = noteList.count { noteWithExercise ->
            val startMillis = dateFromInclusive.epochMillis
            val endMillis = dateToExclusive.epochMillis
            noteWithExercise.noteTimestamp in startMillis until endMillis
                    && noteWithExercise.noteBpm in bpmFromInclusive..bpmToInclusive
        }
        val filter = NoteFilter(
            dateFromInclusive = dateFromInclusive,
            dateToExclusive = dateToExclusive,
            bpmFromInclusive = bpmFromInclusive,
            bpmToInclusive = bpmToInclusive
        )
        val deletedNoteCount = repository.deleteSuspending(
            ids = ids,
            exerciseSubname = null,
            filter = filter
        )
        assertEquals(countByDateAndBpm, deletedNoteCount)
    }
    
    fun testDeletionOtherByIdsAndExerciseSubname() = runBlocking {
        val groupSize = StoreNoteTestHelper.NOTE_COUNT / 3
        val noteDao = dbKeeper.db.noteDao()
        val noteList = noteDao.listPortion(groupSize, 0)
        val ids = noteList.map { it.noteId }

        var deletedNoteCount = repository.deleteOtherSuspending(
            ids = ids,
            exerciseSubname = StoreExerciseTestHelper.EXERCISE_NAME_NOT_CONSISTS_IT
        )
        assertEquals(0, deletedNoteCount)

        val otherNoteList = noteDao.listPortion(
            size = StoreNoteTestHelper.NOTE_COUNT - groupSize,
            offset = groupSize
        )
        val countWithExerciseSubname = otherNoteList.count {
            it.exerciseName
                ?.contains(StoreExerciseTestHelper.EXERCISE_SUBNAME, true)
                ?: false
        }
        deletedNoteCount = repository.deleteOtherSuspending(
            ids = ids,
            exerciseSubname = StoreExerciseTestHelper.EXERCISE_SUBNAME,
            filter = null
        )
        assertEquals(countWithExerciseSubname, deletedNoteCount)
    }

    fun testDeletionOtherByIdsAndDate() = runBlocking {
        val groupSize = StoreNoteTestHelper.NOTE_COUNT / 3
        val noteDao = dbKeeper.db.noteDao()
        val noteList = noteDao.listPortion(groupSize, 0)
        val ids = noteList.map { it.noteId }

        val now = System.currentTimeMillis()
        val testDaysScatter = (0.666f * StoreNoteTestHelper.DAYS_SCATTER).toLong()
        val dateFromInclusive = Days(now - testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)
        val dateToExclusive = Days(now + testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)

        val otherNoteList = noteDao.listPortion(
            size = StoreNoteTestHelper.NOTE_COUNT - groupSize,
            offset = groupSize
        )
        val countByDate = otherNoteList.count { noteWithExercise ->
            val startMillis = dateFromInclusive.epochMillis
            val endMillis = dateToExclusive.epochMillis
            noteWithExercise.noteTimestamp in startMillis until endMillis
        }
        val filter = NoteFilter(
            dateFromInclusive = dateFromInclusive,
            dateToExclusive = dateToExclusive
        )
        val deletedNoteCount = repository.deleteOtherSuspending(
            ids = ids,
            exerciseSubname = null,
            filter = filter
        )
        assertEquals(countByDate, deletedNoteCount)
    }

    fun testDeletionOtherByIdsAndBpm() = runBlocking {
        val groupSize = StoreNoteTestHelper.NOTE_COUNT / 3
        val noteDao = dbKeeper.db.noteDao()
        val noteList = noteDao.listPortion(groupSize, 0)
        val ids = noteList.map { it.noteId }

        val bpmRange = NOTE_BPM_MAX - NOTE_BPM_MIN
        val bpmFromInclusive = NOTE_BPM_MIN + (0.2f * bpmRange).toInt()
        val bpmToInclusive = NOTE_BPM_MAX - (0.3f * bpmRange).toInt()

        val otherNoteList = noteDao.listPortion(
            size = StoreNoteTestHelper.NOTE_COUNT - groupSize,
            offset = groupSize
        )
        val countByBpm = otherNoteList.count {
            it.noteBpm in bpmFromInclusive..bpmToInclusive
        }
        val filter = NoteFilter(
            bpmFromInclusive = bpmFromInclusive,
            bpmToInclusive = bpmToInclusive
        )
        val deletedNoteCount = repository.deleteOtherSuspending(
            ids = ids,
            exerciseSubname = null,
            filter = filter
        )
        assertEquals(countByBpm, deletedNoteCount)
    }

    fun testDeletionOtherByIdsDateAndBpm() = runBlocking {
        val groupSize = StoreNoteTestHelper.NOTE_COUNT / 3
        val noteDao = dbKeeper.db.noteDao()
        val noteList = noteDao.listPortion(groupSize, 0)
        val ids = noteList.map { it.noteId }

        val now = System.currentTimeMillis()
        val testDaysScatter = (0.666f * StoreNoteTestHelper.DAYS_SCATTER).toLong()
        val dateFromInclusive = Days(now - testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)
        val dateToExclusive = Days(now + testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)

        val bpmRange = NOTE_BPM_MAX - NOTE_BPM_MIN
        val bpmFromInclusive = NOTE_BPM_MIN + (0.2f * bpmRange).toInt()
        val bpmToInclusive = NOTE_BPM_MAX - (0.3f * bpmRange).toInt()

        val otherNoteList = noteDao.listPortion(
            size = StoreNoteTestHelper.NOTE_COUNT - groupSize,
            offset = groupSize
        )
        val countByDateAndBpm = otherNoteList.count { noteWithExercise ->
            val startMillis = dateFromInclusive.epochMillis
            val endMillis = dateToExclusive.epochMillis
            noteWithExercise.noteTimestamp in startMillis until endMillis
                    && noteWithExercise.noteBpm in bpmFromInclusive..bpmToInclusive
        }
        val filter = NoteFilter(
            dateFromInclusive = dateFromInclusive,
            dateToExclusive = dateToExclusive,
            bpmFromInclusive = bpmFromInclusive,
            bpmToInclusive = bpmToInclusive
        )
        val deletedNoteCount = repository.deleteOtherSuspending(
            ids = ids,
            exerciseSubname = null,
            filter = filter
        )
        assertEquals(countByDateAndBpm, deletedNoteCount)
    }
}
