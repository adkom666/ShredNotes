package com.adkom666.shrednotes.data.db

import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.common.NO_ID
import com.adkom666.shrednotes.common.toId
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.dao.NoteDao
import com.adkom666.shrednotes.data.db.entity.NoteEntity
import com.adkom666.shrednotes.data.model.NOTE_BPM_MAX
import com.adkom666.shrednotes.data.model.NOTE_BPM_MIN
import kotlin.random.Random

object StoreNoteTestHelper {

    const val NOTE_COUNT = 777
    const val DAYS_SCATTER = 666
    const val MILLIS_PER_DAY = 24 * 3_600_000L

    private const val MILLIS_SCATTER = DAYS_SCATTER * MILLIS_PER_DAY

    fun insertNotes(noteDao: NoteDao, exerciseDao: ExerciseDao) {
        val exerciseEntityList = exerciseDao.list(StoreExerciseTestHelper.EXERCISE_COUNT, 0)
        (1..NOTE_COUNT).forEach { _ ->
            val exerciseEntityIndex = Random.nextInt(StoreExerciseTestHelper.EXERCISE_COUNT)
            val exerciseEntity = exerciseEntityList[exerciseEntityIndex]
            val noteEntity = newNoteEntity(exerciseEntity.id)
            noteDao.insert(noteEntity)
        }
    }

    fun existentExerciseId(exerciseDao: ExerciseDao): Id {
        val exerciseEntityList = exerciseDao.list(1, 0)
        return exerciseEntityList.first().id
    }

    fun nonExistentExerciseId(exerciseDao: ExerciseDao): Id {
        val exerciseEntityList = exerciseDao.list(StoreExerciseTestHelper.EXERCISE_COUNT, 0)
        do {
            val id = Random.nextLong().toId()
            if (exerciseEntityList.map { it.id }.contains(id).not()) {
                return id
            }
        } while (true)
    }

    private fun newNoteEntity(exerciseId: Id): NoteEntity {
        val now = System.currentTimeMillis()
        val timestamp = now + Random.nextLong(
            -MILLIS_SCATTER,
            MILLIS_SCATTER
        )
        return NoteEntity(
            id = NO_ID,
            timestamp = timestamp,
            exerciseId = exerciseId,
            bpm = Random.nextInt(NOTE_BPM_MIN, NOTE_BPM_MAX + 1)
        )
    }
}
