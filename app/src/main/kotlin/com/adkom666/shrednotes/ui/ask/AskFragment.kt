package com.adkom666.shrednotes.ui.ask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.adkom666.shrednotes.databinding.FragmentAskBinding
import dagger.android.support.DaggerFragment

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

    private val binding: FragmentAskBinding
        get() = _binding ?: error("View binding is not initialized!")

    private val model: AskViewModel
        get() = _model ?: error("View model is not initialized!")

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
        _model = ViewModelProvider(this).get(AskViewModel::class.java)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
