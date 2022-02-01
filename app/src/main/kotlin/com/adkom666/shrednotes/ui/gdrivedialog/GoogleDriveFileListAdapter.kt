package com.adkom666.shrednotes.ui.gdrivedialog

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adkom666.shrednotes.common.toId
import com.adkom666.shrednotes.databinding.ItemTitleBinding
import com.adkom666.shrednotes.util.selection.SelectableItems
import timber.log.Timber
import kotlin.properties.Delegates.observable

/**
 * Adapter for interacting with the file list.
 *
 * @property selectableFiles notes to interact.
 * @property onFileClick callback to handle the clicked item.
 */
class GoogleDriveFileListAdapter(
    private val selectableFiles: SelectableItems,
    private val onFileClick: (String) -> Unit
) : ListAdapter<String, GoogleDriveFileListAdapter.ViewHolder>(
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

    /**
     * Whether files selection occurs according file name provided by the [select] function or
     * according [selectableFiles].
     */
    enum class SelectionSource {
        PROVIDED_FILE_NAME,
        SELECTABLE_FILES
    }

    /**
     * Setup current [SelectionSource] or read it.
     */
    var selectionSource: SelectionSource by observable(
        initialValue = SelectionSource.PROVIDED_FILE_NAME
    ) { _, old, new ->
        Timber.d("Change selection source: old=$old, new=$new")
        @SuppressLint("NotifyDataSetChanged")
        if (new != old) {
            notifyDataSetChanged()
        }
    }

    private var selectedFileName: String? = null

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
     * Select the file named [fileName] and return its position if it exists and [selectionSource]
     * is [SelectionSource.PROVIDED_FILE_NAME], or null if it doesn't exists or [selectionSource] is
     * not [SelectionSource.PROVIDED_FILE_NAME].
     *
     * @param fileName name of file to select.
     * @return position of the selected item or null if there is no selected items or
     * [selectionSource] is not [SelectionSource.PROVIDED_FILE_NAME].
     */
    fun select(
        fileName: String
    ): Int? = if (selectionSource == SelectionSource.PROVIDED_FILE_NAME) {
        val oldPositionFound = (0 until itemCount).find {
            getItem(it).equals(selectedFileName, ignoreCase = true)
        }
        selectedFileName = fileName
        val newPositionFound = (0 until itemCount).find {
            getItem(it).equals(fileName, ignoreCase = true)
        }
        oldPositionFound?.let { notifyItemChanged(it) }
        newPositionFound?.let { notifyItemChanged(it) }
        newPositionFound
    } else {
        null
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
         * Fill the file item view with the information from the [fileName].
         *
         * @param fileName information to paste into the file item view.
         */
        fun bind(fileName: String) {
            binding.titleTextView.text = fileName
            val id = absoluteAdapterPosition.toId()
            binding.titleCard.isSelected = when (selectionSource) {
                SelectionSource.PROVIDED_FILE_NAME ->
                    fileName.equals(selectedFileName, ignoreCase = true)
                SelectionSource.SELECTABLE_FILES ->
                    selectableFiles.isSelected(id)
            }
            val handleInactiveFileClick = { onFileClick(fileName) }
            val changeSelection = { isSelected: Boolean ->
                binding.titleCard.isSelected = isSelected
            }
            binding.root.setOnClickListener {
                selectableFiles.click(id, changeSelection, handleInactiveFileClick)
            }
            binding.root.setOnLongClickListener {
                selectableFiles.longClick(id, changeSelection)
                true
            }
        }
    }
}
