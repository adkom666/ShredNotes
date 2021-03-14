package com.adkom666.shrednotes.ui.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.adkom666.shrednotes.data.model.Note
import com.adkom666.shrednotes.databinding.ItemNoteBinding
import com.adkom666.shrednotes.util.selection.SelectableItems
import java.text.DateFormat
import java.util.Locale

/**
 * Adapter for interacting with the note list.
 *
 * @property selectableItems notes to interact.
 * @property onEditNote callback for editing a clicked note.
 */
class NotePagedListAdapter(
    private val selectableItems: SelectableItems,
    private val onEditNote: (Note) -> Unit
) : PagedListAdapter<Note, NotePagedListAdapter.ViewHolder>(DIFF_UTIL_CALLBACK) {

    companion object {

        val DIFF_UTIL_CALLBACK = object : DiffUtil.ItemCallback<Note>() {

            override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
                return oldItem == newItem
            }
        }
    }

    private val dateFormat = DateFormat.getDateTimeInstance(
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
     * Note item [ViewHolder].
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
            binding.noteCard.isSelected = selectableItems.isSelected(note.id)

            val edit = { onEditNote(note) }

            val changeSelection = { isSelected: Boolean ->
                binding.noteCard.isSelected = isSelected
            }

            binding.root.setOnClickListener {
                selectableItems.click(note.id, changeSelection, edit)
            }

            binding.root.setOnLongClickListener {
                selectableItems.longClick(note.id, changeSelection)
                true
            }
        }
    }
}
