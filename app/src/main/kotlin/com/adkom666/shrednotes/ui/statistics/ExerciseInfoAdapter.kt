package com.adkom666.shrednotes.ui.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.adkom666.shrednotes.databinding.ItemExerciseBinding

/**
 * Adapter for interacting with the exercise information list.
 *
 * @property exerciseNames exercise name list to show.
 */
class ExerciseInfoAdapter(
    private val exerciseNames: List<String>
) : RecyclerView.Adapter<ExerciseInfoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemExerciseBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            holder.bind(exerciseNames[position])
        }
    }

    override fun getItemCount(): Int {
        return exerciseNames.size
    }

    /**
     * Records item [ViewHolder].
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
