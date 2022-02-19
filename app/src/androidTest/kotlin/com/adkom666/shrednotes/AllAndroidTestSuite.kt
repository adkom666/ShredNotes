package com.adkom666.shrednotes

import com.adkom666.shrednotes.data.db.StoreExerciseTest
import com.adkom666.shrednotes.data.db.StoreNoteTest
import com.adkom666.shrednotes.data.repository.ExerciseRepositoryTest
import com.adkom666.shrednotes.data.repository.NoteRepositoryTest
import com.adkom666.shrednotes.statistics.CommonStatisticsAggregatorTest
import com.adkom666.shrednotes.statistics.RecordsAggregatorTest
import com.adkom666.shrednotes.statistics.TrackingAggregatorTest
import com.adkom666.shrednotes.statistics.WeekdaysStatisticsAggregatorTest
import org.junit.runner.RunWith
import org.junit.runners.Suite
import kotlin.time.ExperimentalTime

@ExperimentalTime
@RunWith(Suite::class)
@Suite.SuiteClasses(
    StoreExerciseTest::class,
    StoreNoteTest::class,
    ExerciseRepositoryTest::class,
    NoteRepositoryTest::class,
    CommonStatisticsAggregatorTest::class,
    WeekdaysStatisticsAggregatorTest::class,
    RecordsAggregatorTest::class,
    TrackingAggregatorTest::class
)
class AllAndroidTestSuite
