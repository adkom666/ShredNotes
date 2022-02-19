package com.adkom666.shrednotes.ui.statistics

import androidx.lifecycle.ViewModel
import com.adkom666.shrednotes.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * Statistics section model.
 */
class StatisticsViewModel @Inject constructor() : ViewModel() {

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
            R.string.item_statistics_common
        ),
        StatisticsItem(
            StatisticsSection.WEEKDAYS_MAX_BPM,
            R.string.item_statistics_weekdays_max_bpm
        ),
        StatisticsItem(
            StatisticsSection.WEEKDAYS_NOTE_COUNT,
            R.string.item_statistics_weekdays_note_count
        ),
        StatisticsItem(
            StatisticsSection.RECORDS_BPM,
            R.string.item_statistics_records_bpm
        ),
        StatisticsItem(
            StatisticsSection.RECORDS_NOTE_COUNT,
            R.string.item_statistics_records_note_count
        ),
        StatisticsItem(
            StatisticsSection.TRAINING_INTENSITY,
            R.string.item_statistics_tracking_training_intensity
        ),
        StatisticsItem(
            StatisticsSection.PROGRESS,
            R.string.item_statistics_tracking_progress
        )
    )

    /**
     * Collect navigation directions from this flow in the UI thread.
     */
    val navigationFlow: Flow<NavDirection>
        get() = _navigationChannel.receiveAsFlow()

    private val _navigationChannel: Channel<NavDirection> = Channel(1)

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
        _navigationChannel.trySend(direction)
    }
}
