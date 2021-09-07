package com.adkom666.shrednotes.ui.exercises

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.databinding.ItemExerciseBinding
import com.adkom666.shrednotes.util.selection.SelectableItems

/**
 * Adapter for interacting with the exercise list.
 *
 * @property selectableExercises exercises to interact.
 * @property onExerciseClick callback to handle the clicked exercise when the selection is inactive.
 */
class ExercisePagingDataAdapter(
    private val selectableExercises: SelectableItems,
    private val onExerciseClick: (Exercise) -> Unit
) : PagingDataAdapter<Exercise, ExercisePagingDataAdapter.ViewHolder>(DIFF_UTIL_CALLBACK) {

    private companion object {

        private val DIFF_UTIL_CALLBACK = object : DiffUtil.ItemCallback<Exercise>() {

            override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
                return oldItem == newItem
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
     *
     * @property binding exercise view binding.
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
            binding.exerciseCard.isSelected = selectableExercises.isSelected(exercise.id)

            val handleInactiveExerciseClick = { onExerciseClick(exercise) }

            val changeSelection = { isSelected: Boolean ->
                binding.exerciseCard.isSelected = isSelected
            }

            binding.root.setOnClickListener {
                selectableExercises.click(exercise.id, changeSelection, handleInactiveExerciseClick)
            }

            binding.root.setOnLongClickListener {
                selectableExercises.longClick(exercise.id, changeSelection)
                true
            }
        }
    }
}
