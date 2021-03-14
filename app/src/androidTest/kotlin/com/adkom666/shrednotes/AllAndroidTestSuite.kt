package com.adkom666.shrednotes

import com.adkom666.shrednotes.data.db.StoreExerciseTest
import com.adkom666.shrednotes.data.db.StoreNoteTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    StoreExerciseTest::class,
    StoreNoteTest::class
)
class AllAndroidTestSuite
