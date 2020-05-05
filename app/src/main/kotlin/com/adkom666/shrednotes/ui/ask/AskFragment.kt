package com.adkom666.shrednotes.ui.ask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.adkom666.shrednotes.databinding.FragmentAskBinding

/**
 * Ask section sub screen.
 */
class AskFragment : Fragment() {

    companion object {

        /**
         * Preferred way to create a fragment.
         *
         * @return new instance as a [Fragment].
         */
        fun newInstance(): Fragment = AskFragment()
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
    ): View? {
        _binding = FragmentAskBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        _model = ViewModelProvider(this).get(AskViewModel::class.java)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
