package com.adkom666.shrednotes.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.adkom666.shrednotes.databinding.FragmentStatisticsBinding
import dagger.android.support.DaggerFragment

/**
 * Statistics section sub screen.
 */
class StatisticsFragment : DaggerFragment() {

    companion object {

        /**
         * Preferred way to create a fragment.
         *
         * @return new instance as [StatisticsFragment].
         */
        fun newInstance(): StatisticsFragment = StatisticsFragment()
    }

    private val binding: FragmentStatisticsBinding
        get() = _binding ?: error("View binding is not initialized!")

    private val model: StatisticsViewModel
        get() = _model ?: error("View model is not initialized!")

    private var _binding: FragmentStatisticsBinding? = null
    private var _model: StatisticsViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStatisticsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        _model = ViewModelProvider(this).get(StatisticsViewModel::class.java)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
