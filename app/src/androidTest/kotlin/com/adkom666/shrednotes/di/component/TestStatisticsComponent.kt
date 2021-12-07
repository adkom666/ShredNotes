package com.adkom666.shrednotes.di.component

import com.adkom666.shrednotes.di.module.TestStatisticsModule
import com.adkom666.shrednotes.statistics.CommonStatisticsAggregatorTest
import com.adkom666.shrednotes.statistics.RecordsAggregatorTest
import com.adkom666.shrednotes.statistics.TrackingAggregatorTest
import com.adkom666.shrednotes.statistics.WeekdaysStatisticsAggregatorTest
import dagger.Component
import javax.inject.Singleton
import kotlin.time.ExperimentalTime

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@ExperimentalTime
@Singleton
@Component(modules = [TestStatisticsModule::class])
interface TestStatisticsComponent {
    fun inject(test: CommonStatisticsAggregatorTest)
    fun inject(test: WeekdaysStatisticsAggregatorTest)
    fun inject(test: RecordsAggregatorTest)
    fun inject(test: TrackingAggregatorTest)
}
