package com.adkom666.shrednotes.ui.exercises

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.databinding.ItemExerciseBinding
import com.adkom666.shrednotes.util.Selector

/**
 * Adapter for interacting with the exercise list.
 *
 * @property selector information about the selected exercises.
 * @property onEditExercise callback for editing a clicked exercise.
 */
class ExercisePagedListAdapter(
    private val selector: Selector,
    private val onEditExercise: (Exercise) -> Unit
) : PagedListAdapter<Exercise, ExercisePagedListAdapter.ViewHolder>(DIFF_UTIL_CALLBACK) {

    companion object {

        val DIFF_UTIL_CALLBACK = object : DiffUtil.ItemCallback<Exercise>() {

            override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemExerciseBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            getItem(position)?.let { exercise ->
                holder.bind(exercise)
            }
        }
    }

    /**
     * Exercise item [ViewHolder].
     */
    inner class ViewHolder(
        private val binding: ItemExerciseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Fill the exercise item view with the information from the [exercise].
         *
         * @param exercise information to paste into the exercise item view.
         */
        fun bind(exercise: Exercise) {
            binding.exerciseNameTextView.text = exercise.name
            binding.exerciseCard.isSelected = selector.isSelected(exercise.id)

            val edit = { onEditExercise(exercise) }

            val changeSelection = { isSelected: Boolean ->
                binding.exerciseCard.isSelected = isSelected
            }

            binding.root.setOnClickListener {
                selector.click(exercise.id, edit, changeSelection)
            }

            binding.root.setOnLongClickListener {
                selector.longClick(exercise.id, changeSelection)
                true
            }
        }
    }
}
