package com.adkom666.shrednotes.data.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry

class TestDbKeeper {

    val db: ShredNotesDatabase
        get() = requireNotNull(_db)

    private var _db: ShredNotesDatabase? = null

    fun createDb() {
        _db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ShredNotesDatabase::class.java
        ).build()
    }

    fun destroyDb() {
        db.close()
        _db = null
    }
}
