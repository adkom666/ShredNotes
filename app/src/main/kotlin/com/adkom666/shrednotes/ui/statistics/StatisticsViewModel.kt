package com.adkom666.shrednotes.ui.statistics

import androidx.lifecycle.ViewModel
import com.adkom666.shrednotes.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import timber.log.Timber
import javax.inject.Inject

/**
 * Statistics section model.
 */
@ExperimentalCoroutinesApi
class StatisticsViewModel @Inject constructor() : ViewModel() {

    private companion object {
        private const val NAVIGATION_CHANNEL_CAPACITY = 1
    }

    /**
     * Navigation direction.
     */
    sealed class NavDirection {

        /**
         * To the screen with statistics.
         *
         * @property statisticsSection identifier of statistics screen.
         */
        data class ToStatisticsScreen(val statisticsSection: StatisticsSection) : NavDirection()
    }

    /**
     * Statistic items to interact.
     */
    val statisticsList: List<StatisticsItem> = listOf(
        StatisticsItem(
            StatisticsSection.COMMON,
            R.string.title_statistics_common
        ),
        StatisticsItem(
            StatisticsSection.BY_DAYS_OF_WEEK,
            R.string.title_statistics_by_days_of_week
        )
    )

    /**
     * Consume navigation directions from this channel in the UI thread.
     */
    val navigationChannel: ReceiveChannel<NavDirection>
        get() = _navigationChannel.openSubscription()

    private val _navigationChannel: BroadcastChannel<NavDirection> =
        BroadcastChannel(NAVIGATION_CHANNEL_CAPACITY)

    /**
     * Call to handle the statistics item when it clicked.
     *
     * @param statisticsSection corresponding statistics section.
     */
    fun onStatisticsItemClick(statisticsSection: StatisticsSection) {
        Timber.d("On statistics item click: statisticsSection=$statisticsSection")
        navigateTo(NavDirection.ToStatisticsScreen(statisticsSection))
    }

    private fun navigateTo(direction: NavDirection) {
        Timber.d("Navigate to: direction=$direction")
        _navigationChannel.offer(direction)
    }
}
