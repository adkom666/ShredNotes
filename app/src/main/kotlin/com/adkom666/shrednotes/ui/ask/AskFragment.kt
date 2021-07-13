package com.adkom666.shrednotes.ui.ask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.adkom666.shrednotes.databinding.FragmentAskBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import dagger.android.support.DaggerFragment
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        binding.donateImageButton.setOnClickListener {
            model.donate()
        }
    }

    private fun observeLiveData() {
        val stateObserver = StateObserver()
        model.stateAsLiveData.observe(viewLifecycleOwner, stateObserver)
    }

    private inner class StateObserver : Observer<AskViewModel.State> {

        override fun onChanged(state: AskViewModel.State?) {
            Timber.d("State is $state")
            when (state) {
                AskViewModel.State.Loading -> {
                    binding.donateImageButton.isEnabled = false
                    binding.donationPriceTextView.isVisible = false
                    binding.donationPriceProgressBar.isVisible = true
                }
                is AskViewModel.State.Preparation -> {
                    binding.donationPriceTextView.text = state.price
                    model.ok()
                }
                AskViewModel.State.Asking -> {
                    binding.donateImageButton.isEnabled = true
                    binding.donationPriceTextView.isVisible = true
                    binding.donationPriceTextView.isEnabled = true
                    binding.donationPriceProgressBar.isVisible = false
                }
                AskViewModel.State.Donation -> {
                    binding.donateImageButton.isEnabled = false
                    binding.donationPriceTextView.isVisible = true
                    binding.donationPriceTextView.isEnabled = false
                    binding.donationPriceProgressBar.isVisible = false
                }
            }
        }
    }
}
