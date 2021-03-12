package com.adkom666.shrednotes.data.db

import com.adkom666.shrednotes.common.Id
import com.adkom666.shrednotes.common.NO_ID
import com.adkom666.shrednotes.common.toId
import com.adkom666.shrednotes.data.db.dao.ExerciseDao
import com.adkom666.shrednotes.data.db.dao.NoteDao
import com.adkom666.shrednotes.data.db.entity.NoteEntity
import com.adkom666.shrednotes.data.model.NOTE_BPM_MAX
import com.adkom666.shrednotes.data.model.NOTE_BPM_MIN
import java.util.Date
import kotlin.random.Random

object StoreNoteTestHelper {

    const val NOTE_COUNT = 777

    fun insertNotes(noteDao: NoteDao, exerciseDao: ExerciseDao) {
        val exerciseEntityList = exerciseDao.entities(StoreExerciseTestHelper.EXERCISE_COUNT, 0)
        (1..NOTE_COUNT).forEach { _ ->
            val exerciseEntityIndex = Random.nextInt(StoreExerciseTestHelper.EXERCISE_COUNT)
            val exerciseEntity = exerciseEntityList[exerciseEntityIndex]
            val noteEntity = newNoteEntity(exerciseEntity.id)
            noteDao.insert(noteEntity)
        }
    }

    fun existentExerciseId(exerciseDao: ExerciseDao): Id {
        val exerciseEntityList = exerciseDao.entities(1, 0)
        return exerciseEntityList.first().id
    }

    fun nonExistentExerciseId(exerciseDao: ExerciseDao): Id {
        val exerciseEntityList = exerciseDao.entities(StoreExerciseTestHelper.EXERCISE_COUNT, 0)
        do {
            val id = Random.nextLong().toId()
            if (exerciseEntityList.map { it.id }.contains(id).not()) {
                return id
            }
        } while (true)
    }

    private fun newNoteEntity(exerciseId: Id): NoteEntity {
        return NoteEntity(
            id = NO_ID,
            dateTime = Date(),
            exerciseId = exerciseId,
            bpm = Random.nextInt(NOTE_BPM_MIN, NOTE_BPM_MAX + 1)
        )
    }
}
