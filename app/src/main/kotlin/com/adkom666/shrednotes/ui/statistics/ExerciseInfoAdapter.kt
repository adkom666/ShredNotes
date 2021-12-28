package com.adkom666.shrednotes.ui.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adkom666.shrednotes.databinding.ItemExerciseBinding

/**
 * Adapter for interacting with the exercise information list.
 */
class ExerciseInfoAdapter : ListAdapter<String, ExerciseInfoAdapter.ViewHolder>(
    DIFF_UTIL_CALLBACK
) {

    private companion object {

        private val DIFF_UTIL_CALLBACK = object : DiffUtil.ItemCallback<String>() {

            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
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
            getItem(position)?.let { exerciseName ->
                holder.bind(exerciseName)
            }
        }
    }

    /**
     * Records item [ViewHolder].
     *
     * @property binding exercise view binding.
     */
    inner class ViewHolder(
        private val binding: ItemExerciseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Fill the exercise item view with the information from the [exerciseName].
         *
         * @param exerciseName information to paste into the item view as exercise name.
         */
        fun bind(exerciseName: String) {
            binding.exerciseNameTextView.text = exerciseName
            binding.exerciseCard.isSelected = false
            binding.exerciseCard.isFocusable = false
            binding.exerciseCard.isClickable = false
        }
    }
}
