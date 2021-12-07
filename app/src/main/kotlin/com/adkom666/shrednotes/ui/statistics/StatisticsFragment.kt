package com.adkom666.shrednotes.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.databinding.FragmentStatisticsBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.util.FirstItemDecoration
import dagger.android.support.DaggerFragment
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach

/**
 * Statistics section sub screen.
 */
@ExperimentalCoroutinesApi
@ExperimentalTime
class StatisticsFragment : DaggerFragment() {

    companion object {

        /**
         * Preferred way to create a fragment.
         *
         * @return new instance as [StatisticsFragment].
         */
        fun newInstance(): StatisticsFragment = StatisticsFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val binding: FragmentStatisticsBinding
        get() = requireNotNull(_binding)

    private val model: StatisticsViewModel
        get() = requireNotNull(_model)

    private var _binding: FragmentStatisticsBinding? = null
    private var _model: StatisticsViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _model = viewModel(viewModelFactory)

        initStatisticsRecycler()
        listenChannels()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initStatisticsRecycler() {
        val llm = LinearLayoutManager(context)
        llm.orientation = LinearLayoutManager.VERTICAL
        binding.statisticsRecycler.layoutManager = llm
        val marginTop = resources.getDimension(R.dimen.card_vertical_margin)
        val decoration = FirstItemDecoration(marginTop.toInt())
        binding.statisticsRecycler.addItemDecoration(decoration)
        val adapter = StatisticsAdapter(model.statisticsList) { statisticsSection ->
            Timber.d("Statistics title clicked: statisticsSection=$statisticsSection")
            model.onStatisticsItemClick(statisticsSection)
        }
        binding.statisticsRecycler.adapter = adapter
    }

    private fun listenChannels() {
        lifecycleScope.launchWhenResumed {
            model.navigationChannel.consumeEach(::goToScreen)
        }
    }

    private fun goToScreen(direction: StatisticsViewModel.NavDirection) = when (direction) {
        is StatisticsViewModel.NavDirection.ToStatisticsScreen ->
            goToStatisticsScreen(direction.statisticsSection)
    }

    private fun goToStatisticsScreen(statisticsSection: StatisticsSection) {
        context?.let { safeContext ->
            val intent = when (statisticsSection) {
                StatisticsSection.COMMON ->
                    CommonStatisticsActivity.newIntent(safeContext)
                StatisticsSection.WEEKDAYS_MAX_BPM ->
                    WeekdaysStatisticsActivity.newIntent(
                        safeContext,
                        WeekdaysStatisticsTargetParameter.MAX_BPM
                    )
                StatisticsSection.WEEKDAYS_NOTE_COUNT ->
                    WeekdaysStatisticsActivity.newIntent(
                        safeContext,
                        WeekdaysStatisticsTargetParameter.NOTE_COUNT
                    )
                StatisticsSection.RECORDS_BPM ->
                    RecordsActivity.newIntent(
                        safeContext,
                        RecordsTargetParameter.BPM
                    )
                StatisticsSection.RECORDS_NOTE_COUNT ->
                    RecordsActivity.newIntent(
                        safeContext,
                        RecordsTargetParameter.NOTE_COUNT
                    )
                StatisticsSection.TRAINING_INTENSITY ->
                    TrackingActivity.newIntent(
                        safeContext,
                        TrackingTargetParameter.NOTE_COUNT
                    )
                StatisticsSection.PROGRESS ->
                    TrackingActivity.newIntent(
                        safeContext,
                        TrackingTargetParameter.MAX_BPM
                    )
            }
            startActivity(intent)
        }
    }
}
