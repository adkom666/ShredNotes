package com.adkom666.shrednotes.ui.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.databinding.ItemNoteBinding
import java.text.DateFormat
import java.util.Locale

/**
 * Adapter for interacting with the note list.
 */
class NoteAdapter : ListAdapter<Note, NoteAdapter.ViewHolder>(
    DIFF_UTIL_CALLBACK
) {
    private companion object {

        private val DIFF_UTIL_CALLBACK = object : DiffUtil.ItemCallback<Note>() {

            override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
                return oldItem == newItem
            }
        }
    }

    private val dateFormat: DateFormat = DateFormat.getDateTimeInstance(
        DateFormat.SHORT,
        DateFormat.SHORT,
        Locale.getDefault()
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemNoteBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            getItem(position)?.let { note ->
                holder.bind(note)
            }
        }
    }

    /**
     * Records item [ViewHolder].
     *
     * @property binding note view binding.
     */
    inner class ViewHolder(
        private val binding: ItemNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Fill the note item view with the information from the [note].
         *
         * @param note information to paste into the note item view.
         */
        fun bind(note: Note) {
            binding.dateTimeTextView.text = dateFormat.format(note.dateTime.date)
            binding.exerciseNameTextView.text = note.exerciseName
            binding.bpmTextView.text = note.bpm.toString()
            binding.noteCard.isSelected = false
            binding.noteCard.isFocusable = false
            binding.noteCard.isClickable = false
        }
    }
}
