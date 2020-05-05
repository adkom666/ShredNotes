package com.adkom666.shrednotes.ui.exercises

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.adkom666.shrednotes.databinding.FragmentExercisesBinding
import com.adkom666.shrednotes.ui.Searchable

/**
 * Exercises section sub screen.
 */
class ExercisesFragment :
    Fragment(),
    Searchable {

    companion object {

        /**
         * Preferred way to create a fragment.
         *
         * @return new instance as a [Fragment].
         */
        fun newInstance(): Fragment = ExercisesFragment()
    }

    private val binding: FragmentExercisesBinding
        get() = _binding ?: error("View binding is not initialized!")

    private val model: ExercisesViewModel
        get() = _model ?: error("View model is not initialized!")

    private var _binding: FragmentExercisesBinding? = null
    private var _model: ExercisesViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentExercisesBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        _model = ViewModelProvider(this).get(ExercisesViewModel::class.java)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun search(query: String?): Boolean {
        // Dummy
        return false
    }

    override fun preview(newText: String?): Boolean {
        // Dummy
        return false
    }
}
