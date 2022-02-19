package com.adkom666.shrednotes.data.repository

import com.adkom666.shrednotes.data.db.ShredNotesDatabase
import com.adkom666.shrednotes.data.db.StoreExerciseTestHelper
import com.adkom666.shrednotes.data.db.StoreNoteTestHelper
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.dao.NoteDao
import com.adkom666.shrednotes.data.model.NOTE_BPM_MAX
import com.adkom666.shrednotes.data.model.NOTE_BPM_MIN
import com.adkom666.shrednotes.data.model.NoteFilter
import com.adkom666.shrednotes.di.component.DaggerTestRepositoryComponent
import com.adkom666.shrednotes.util.time.Days
import javax.inject.Inject
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class NoteRepositoryTest : TestCase() {

    @Inject
    lateinit var database: ShredNotesDatabase

    @Inject
    lateinit var exerciseDao: ExerciseDao

    @Inject
    lateinit var noteDao: NoteDao

    @Inject
    lateinit var repository: NoteRepository

    public override fun setUp() {
        super.setUp()
        DaggerTestRepositoryComponent.create().inject(this)
        StoreExerciseTestHelper.insertExercises(exerciseDao)
        StoreNoteTestHelper.insertNotes(noteDao, exerciseDao)
    }

    public override fun tearDown() {
        super.tearDown()
        database.close()
    }

    fun testCountWithoutParameters() = runBlocking {
        val count = repository.countSuspending(
            exerciseSubname = null,
            filter = null
        )
        assertEquals(StoreNoteTestHelper.NOTE_COUNT, count)
    }

    fun testCountByExerciseSubname() = runBlocking {
        val noteList = noteDao.list(StoreNoteTestHelper.NOTE_COUNT, 0)
        val countByExerciseSubname = noteList.count {
            it.exerciseName
                ?.contains(StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME, true)
                ?: false
        }
        val count = repository.countSuspending(
            exerciseSubname = StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME,
            filter = null
        )
        assertEquals(countByExerciseSubname, count)
    }

    fun testCountByDate() = runBlocking {
        val now = System.currentTimeMillis()
        val testDaysScatter = (0.666f * StoreNoteTestHelper.DAYS_SCATTER).toLong()
        val dateFromInclusive = Days(now - testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)
        val dateToExclusive = Days(now + testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)

        val noteList = noteDao.list(StoreNoteTestHelper.NOTE_COUNT, 0)
        val calendar = Calendar.getInstance()
        val zoneOffset = calendar.get(Calendar.ZONE_OFFSET)
        val countByDate = noteList.count { noteWithExercise ->
            val startMillis = dateFromInclusive.epochMillis + zoneOffset
            val endMillis = dateToExclusive.epochMillis + zoneOffset
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
        val bpmRange = NOTE_BPM_MAX - NOTE_BPM_MIN
        val bpmFromInclusive = NOTE_BPM_MIN + (0.2f * bpmRange).toInt()
        val bpmToInclusive = NOTE_BPM_MAX - (0.3f * bpmRange).toInt()

        val noteList = noteDao.list(StoreNoteTestHelper.NOTE_COUNT, 0)
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
        val now = System.currentTimeMillis()
        val testDaysScatter = (0.666f * StoreNoteTestHelper.DAYS_SCATTER).toLong()
        val dateFromInclusive = Days(now - testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)
        val dateToExclusive = Days(now + testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)

        val bpmRange = NOTE_BPM_MAX - NOTE_BPM_MIN
        val bpmFromInclusive = NOTE_BPM_MIN + (0.2f * bpmRange).toInt()
        val bpmToInclusive = NOTE_BPM_MAX - (0.3f * bpmRange).toInt()

        val noteList = noteDao.list(StoreNoteTestHelper.NOTE_COUNT, 0)
        val calendar = Calendar.getInstance()
        val zoneOffset = calendar.get(Calendar.ZONE_OFFSET)
        val countByExerciseSubnameDateAndBpm = noteList.count { noteWithExercise ->
            val startMillis = dateFromInclusive.epochMillis + zoneOffset
            val endMillis = dateToExclusive.epochMillis + zoneOffset
            (noteWithExercise.exerciseName
                ?.contains(StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME, true)
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
            exerciseSubname = StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME,
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
        val noteList = noteDao.list(StoreNoteTestHelper.NOTE_COUNT, 0)
        val countByExerciseSubname = noteList.count {
            it.exerciseName
                ?.contains(StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME, true)
                ?: false
        }
        val list = repository.list(
            size = StoreNoteTestHelper.NOTE_COUNT,
            startPosition = 0,
            exerciseSubname = StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME,
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

    fun testPageWithTranscendentalStartPosition() = runBlocking {
        val pageSize = StoreNoteTestHelper.NOTE_COUNT / 3
        val page = repository.page(
            size = pageSize,
            requestedStartPosition = StoreNoteTestHelper.NOTE_COUNT * 666,
            exerciseSubname = null,
            filter = null
        )
        assertEquals(pageSize, page.items.size)
        assertEquals(StoreNoteTestHelper.NOTE_COUNT - pageSize, page.offset)
    }

    fun testDeletionByIdsAndExerciseSubname() = runBlocking {
        val groupSize = StoreNoteTestHelper.NOTE_COUNT / 3
        val noteList = noteDao.list(groupSize, 0)
        val ids = noteList.map { it.noteId }

        var deletedNoteCount = repository.deleteSuspending(
            ids = ids,
            exerciseSubname = StoreExerciseTestHelper.EXERCISE_NAME_NOT_CONSISTS_IT,
            filter = null
        )
        assertEquals(0, deletedNoteCount)

        val countWithExerciseSubname = noteList.count {
            it.exerciseName
                ?.contains(StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME, true)
                ?: false
        }
        deletedNoteCount = repository.deleteSuspending(
            ids = ids,
            exerciseSubname = StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME,
            filter = null
        )
        assertEquals(countWithExerciseSubname, deletedNoteCount)
    }

    fun testDeletionByIdsAndDate() = runBlocking {
        val groupSize = StoreNoteTestHelper.NOTE_COUNT / 3
        val noteList = noteDao.list(groupSize, 0)
        val ids = noteList.map { it.noteId }

        val now = System.currentTimeMillis()
        val testDaysScatter = (0.666f * StoreNoteTestHelper.DAYS_SCATTER).toLong()
        val dateFromInclusive = Days(now - testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)
        val dateToExclusive = Days(now + testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)

        val calendar = Calendar.getInstance()
        val zoneOffset = calendar.get(Calendar.ZONE_OFFSET)
        val countByDate = noteList.count { noteWithExercise ->
            val startMillis = dateFromInclusive.epochMillis + zoneOffset
            val endMillis = dateToExclusive.epochMillis + zoneOffset
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
        val noteList = noteDao.list(groupSize, 0)
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
        val noteList = noteDao.list(groupSize, 0)
        val ids = noteList.map { it.noteId }

        val now = System.currentTimeMillis()
        val testDaysScatter = (0.666f * StoreNoteTestHelper.DAYS_SCATTER).toLong()
        val dateFromInclusive = Days(now - testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)
        val dateToExclusive = Days(now + testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)

        val bpmRange = NOTE_BPM_MAX - NOTE_BPM_MIN
        val bpmFromInclusive = NOTE_BPM_MIN + (0.2f * bpmRange).toInt()
        val bpmToInclusive = NOTE_BPM_MAX - (0.3f * bpmRange).toInt()

        val calendar = Calendar.getInstance()
        val zoneOffset = calendar.get(Calendar.ZONE_OFFSET)
        val countByDateAndBpm = noteList.count { noteWithExercise ->
            val startMillis = dateFromInclusive.epochMillis + zoneOffset
            val endMillis = dateToExclusive.epochMillis + zoneOffset
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
        val noteList = noteDao.list(groupSize, 0)
        val ids = noteList.map { it.noteId }

        var deletedNoteCount = repository.deleteOtherSuspending(
            ids = ids,
            exerciseSubname = StoreExerciseTestHelper.EXERCISE_NAME_NOT_CONSISTS_IT
        )
        assertEquals(0, deletedNoteCount)

        val otherNoteList = noteDao.list(
            size = StoreNoteTestHelper.NOTE_COUNT - groupSize,
            offset = groupSize
        )
        val countWithExerciseSubname = otherNoteList.count {
            it.exerciseName
                ?.contains(StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME, true)
                ?: false
        }
        deletedNoteCount = repository.deleteOtherSuspending(
            ids = ids,
            exerciseSubname = StoreExerciseTestHelper.PROBABLE_EXERCISE_SUBNAME,
            filter = null
        )
        assertEquals(countWithExerciseSubname, deletedNoteCount)
    }

    fun testDeletionOtherByIdsAndDate() = runBlocking {
        val groupSize = StoreNoteTestHelper.NOTE_COUNT / 3
        val noteList = noteDao.list(groupSize, 0)
        val ids = noteList.map { it.noteId }

        val now = System.currentTimeMillis()
        val testDaysScatter = (0.666f * StoreNoteTestHelper.DAYS_SCATTER).toLong()
        val dateFromInclusive = Days(now - testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)
        val dateToExclusive = Days(now + testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)

        val otherNoteList = noteDao.list(
            size = StoreNoteTestHelper.NOTE_COUNT - groupSize,
            offset = groupSize
        )
        val calendar = Calendar.getInstance()
        val zoneOffset = calendar.get(Calendar.ZONE_OFFSET)
        val countByDate = otherNoteList.count { noteWithExercise ->
            val startMillis = dateFromInclusive.epochMillis + zoneOffset
            val endMillis = dateToExclusive.epochMillis + zoneOffset
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
        val noteList = noteDao.list(groupSize, 0)
        val ids = noteList.map { it.noteId }

        val bpmRange = NOTE_BPM_MAX - NOTE_BPM_MIN
        val bpmFromInclusive = NOTE_BPM_MIN + (0.2f * bpmRange).toInt()
        val bpmToInclusive = NOTE_BPM_MAX - (0.3f * bpmRange).toInt()

        val otherNoteList = noteDao.list(
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
        val noteList = noteDao.list(groupSize, 0)
        val ids = noteList.map { it.noteId }

        val now = System.currentTimeMillis()
        val testDaysScatter = (0.666f * StoreNoteTestHelper.DAYS_SCATTER).toLong()
        val dateFromInclusive = Days(now - testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)
        val dateToExclusive = Days(now + testDaysScatter * StoreNoteTestHelper.MILLIS_PER_DAY)

        val bpmRange = NOTE_BPM_MAX - NOTE_BPM_MIN
        val bpmFromInclusive = NOTE_BPM_MIN + (0.2f * bpmRange).toInt()
        val bpmToInclusive = NOTE_BPM_MAX - (0.3f * bpmRange).toInt()

        val otherNoteList = noteDao.list(
            size = StoreNoteTestHelper.NOTE_COUNT - groupSize,
            offset = groupSize
        )
        val calendar = Calendar.getInstance()
        val zoneOffset = calendar.get(Calendar.ZONE_OFFSET)
        val countByDateAndBpm = otherNoteList.count { noteWithExercise ->
            val startMillis = dateFromInclusive.epochMillis + zoneOffset
            val endMillis = dateToExclusive.epochMillis + zoneOffset
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
