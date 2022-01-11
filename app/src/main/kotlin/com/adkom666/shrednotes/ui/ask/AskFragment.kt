package com.adkom666.shrednotes.ui.ask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.adkom666.shrednotes.databinding.FragmentAskBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject

/**
 * Ask section sub screen.
 */
class AskFragment : DaggerFragment() {

    companion object {

        /**
         * Preferred way to create a fragment.
         *
         * @return new instance as [AskFragment].
         */
        fun newInstance(): AskFragment = AskFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val binding: FragmentAskBinding
        get() = requireNotNull(_binding)

    private val model: AskViewModel
        get() = requireNotNull(_model)

    private var _binding: FragmentAskBinding? = null
    private var _model: AskViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAskBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _model = viewModel(viewModelFactory)

        setupListeners()
        observeLiveData()
        listenFlows()

        context?.let {
            model.prepareDonor(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        binding.donateImageButton.setOnClickListener {
            activity?.let {
                model.donate(it)
            }
        }
        binding.refreshDonationPriceButton.setOnClickListener {
            context?.let {
                model.forcePrepareDonor(it)
            }
        }
    }

    private fun observeLiveData() {
        val stateObserver = StateObserver()
        model.stateAsLiveData.observe(viewLifecycleOwner, stateObserver)
    }

    private fun listenFlows() {
        lifecycleScope.launchWhenCreated {
            model.signalFlow.collect(::process)
        }
    }

    private fun process(signal: AskViewModel.Signal) {
        Timber.d("Signal is $signal")
        when (signal) {
            is AskViewModel.Signal.DonationPrice ->
                binding.donationPriceTextView.text = signal.value
        }
    }

    private inner class StateObserver : Observer<AskViewModel.State> {

        override fun onChanged(state: AskViewModel.State?) {
            Timber.d("State is $state")
            binding.donateImageButton.isEnabled =
                state == AskViewModel.State.Asking
            binding.donationPriceTextView.isInvisible =
                state != AskViewModel.State.Asking && state != AskViewModel.State.NotAsking
            binding.donationPriceTextView.isEnabled =
                state == AskViewModel.State.Asking
            binding.donationPriceProgressBar.isInvisible =
                state != AskViewModel.State.Loading
            binding.refreshDonationPriceButton.isInvisible =
                state != AskViewModel.State.UnknownDonationPrice
        }
    }
}
