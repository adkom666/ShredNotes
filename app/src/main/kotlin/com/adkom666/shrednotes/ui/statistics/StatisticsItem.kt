package com.adkom666.shrednotes.ui.statistics

import androidx.annotation.StringRes

/**
 * Binding a statistics section to its title.
 *
 * @property statisticsSection see [StatisticsSection].
 * @property titleResId the resource identifier of the string resource to be displayed as title.
 */
data class StatisticsItem(
    val statisticsSection: StatisticsSection,
    @StringRes
    val titleResId: Int
)
