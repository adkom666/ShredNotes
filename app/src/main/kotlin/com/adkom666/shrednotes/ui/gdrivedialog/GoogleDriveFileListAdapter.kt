package com.adkom666.shrednotes.ui.gdrivedialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adkom666.shrednotes.databinding.ItemTitleBinding

/**
 * Adapter for interacting with the file list.
 *
 * @property onFileClick callback to handle the clicked item.
 */
class GoogleDriveFileListAdapter(
    private val onFileClick: (String) -> Unit
) : ListAdapter<SelectableFileName, GoogleDriveFileListAdapter.ViewHolder>(
    DIFF_UTIL_CALLBACK
) {
    private companion object {

        private val DIFF_UTIL_CALLBACK = object : DiffUtil.ItemCallback<SelectableFileName>() {

            override fun areItemsTheSame(
                oldItem: SelectableFileName,
                newItem: SelectableFileName
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }

            override fun areContentsTheSame(
                oldItem: SelectableFileName,
                newItem: SelectableFileName
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTitleBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            getItem(position)?.let { selectableFileName ->
                holder.bind(selectableFileName)
            }
        }
    }

    /**
     * Select the file named [fileName] and return its position if it exists, or null if it doesn't
     * exists.
     *
     * @param fileName name of file to select.
     * @return position of the selected item or null if there is no selected items.
     */
    fun select(fileName: String): Int? {
        val oldPositionFound = (0 until itemCount).find {
            getItem(it).isSelected
        }
        val newPositionFound = (0 until itemCount).find {
            getItem(it).fileName == fileName
        }
        oldPositionFound?.let { oldPosition ->
            if (oldPosition != newPositionFound) {
                getItem(oldPosition).isSelected = false
                notifyItemChanged(oldPosition)
            }
        }
        newPositionFound?.let { newPosition ->
            if (newPosition != oldPositionFound) {
                getItem(newPosition).isSelected = true
                notifyItemChanged(newPosition)
            }
        }
        return newPositionFound
    }

    /**
     * File item [ViewHolder].
     *
     * @property binding title view binding.
     */
    inner class ViewHolder(
        private val binding: ItemTitleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Fill the file item view with the information from the [selectableFileName].
         *
         * @param selectableFileName information to paste into the file item view.
         */
        fun bind(selectableFileName: SelectableFileName) {
            binding.titleTextView.text = selectableFileName.fileName
            binding.root.isSelected = selectableFileName.isSelected
            binding.root.setOnClickListener {
                onFileClick(selectableFileName.fileName)
            }
        }
    }
}
