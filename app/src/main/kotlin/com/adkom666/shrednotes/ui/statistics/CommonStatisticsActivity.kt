package com.adkom666.shrednotes.ui.statistics

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.databinding.ActivityStatisticsCommonBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.statistics.CommonStatistics
import com.adkom666.shrednotes.util.toast
import dagger.android.AndroidInjection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Common statistics screen.
 */
@ExperimentalCoroutinesApi
class CommonStatisticsActivity : AppCompatActivity() {

    companion object {

        private val LINE_SEPARATOR = System.getProperty("line.separator")

        /**
         * Creating an intent to open the common statistics screen.
         *
         * @param context [Context] to create an intent.
         * @return intent to open the common statistics screen.
         */
        fun newIntent(context: Context): Intent {
            return Intent(context, CommonStatisticsActivity::class.java)
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val binding: ActivityStatisticsCommonBinding
        get() = requireNotNull(_binding)

    private val model: CommonStatisticsViewModel
        get() = requireNotNull(_model)

    private var _binding: ActivityStatisticsCommonBinding? = null
    private var _model: CommonStatisticsViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        _binding = ActivityStatisticsCommonBinding.inflate(layoutInflater)
        setContentView(binding.root)
        _model = viewModel(viewModelFactory)

        setupButtonListeners()
        observeLiveData()
        listenChannels()

        val isFirstStart = savedInstanceState == null
        if (isFirstStart) {
            model.prepare()
        }
        lifecycleScope.launchWhenCreated {
            model.start()
        }
    }

    private fun setupButtonListeners() {
        binding.okButton.setOnClickListener {
            Timber.d("OK clicked")
            model.onOkButtonClick()
        }
    }

    private fun observeLiveData() {
        model.stateAsLiveData.observe(this, StateObserver())
    }

    private fun listenChannels() {
        lifecycleScope.launchWhenStarted {
            model.messageChannel.consumeEach(::show)
        }
        lifecycleScope.launch {
            model.signalChannel.consumeEach(::process)
        }
    }

    private fun show(message: CommonStatisticsViewModel.Message) = when (message) {
        is CommonStatisticsViewModel.Message.Error -> showError(message)
    }

    private fun showError(message: CommonStatisticsViewModel.Message.Error) = when (message) {
        is CommonStatisticsViewModel.Message.Error.Clarified -> {
            val messageString = getString(
                R.string.error_clarified,
                message.details
            )
            toast(messageString)
        }
        CommonStatisticsViewModel.Message.Error.Unknown ->
            toast(R.string.error_unknown)
    }

    private fun process(signal: CommonStatisticsViewModel.Signal) {
        Timber.d("Signal is $signal")
        when (signal) {
            is CommonStatisticsViewModel.Signal.Statistics ->
                setStatistics(signal.value)
        }
    }

    private fun setStatistics(statistics: CommonStatistics) {
        val line001 = getString(
            R.string.text_statistics_common_line_001,
            statistics.totalNotes
        )
        val line002 = getString(
            R.string.text_statistics_common_line_002,
            statistics.totalExercises
        )
        val text = line001 + LINE_SEPARATOR + line002
        binding.statisticsTextView.text = text
    }

    private inner class StateObserver : Observer<CommonStatisticsViewModel.State> {

        override fun onChanged(state: CommonStatisticsViewModel.State?) {
            Timber.d("State is $state")
            when (state) {
                CommonStatisticsViewModel.State.Waiting ->
                    setWaiting()
                CommonStatisticsViewModel.State.Working ->
                    setWorking()
                CommonStatisticsViewModel.State.Finishing ->
                    finish()
            }
        }

        private fun setWaiting() = setProgressActive(true)
        private fun setWorking() = setProgressActive(false)

        private fun setProgressActive(isActive: Boolean) {
            binding.progressBar.isVisible = isActive
            binding.statisticsCard.isVisible = isActive.not()
        }
    }
}
