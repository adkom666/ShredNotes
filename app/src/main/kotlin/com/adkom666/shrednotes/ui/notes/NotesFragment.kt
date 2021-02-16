package com.adkom666.shrednotes.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.adkom666.shrednotes.databinding.FragmentNotesBinding
import com.adkom666.shrednotes.ui.Filterable
import com.adkom666.shrednotes.ui.Searchable

/**
 * Notes section sub screen.
 */
class NotesFragment :
    Fragment(),
    Searchable,
    Filterable {

    companion object {

        /**
         * Preferred way to create a fragment.
         *
         * @return new instance as a [Fragment].
         */
        fun newInstance(): Fragment = NotesFragment()
    }

    private val binding: FragmentNotesBinding
        get() = _binding ?: error("View binding is not initialized!")

    private val model: NotesViewModel
        get() = _model ?: error("View model is not initialized!")

    private var _binding: FragmentNotesBinding? = null
    private var _model: NotesViewModel? = null

    override var isSearchActive: Boolean = false

    override val currentQuery: String?
        get() = null

    override val isFilterEnabled: Boolean
        get() = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNotesBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        _model = ViewModelProvider(this).get(NotesViewModel::class.java)
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

    override fun filter(onFilter: (filterEnabled: Boolean) -> Unit) {
        // Dummy
        onFilter(true)
    }
}
