package com.adkom666.shrednotes.ui.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.adkom666.shrednotes.databinding.ItemTitleBinding

/**
 * Adapter for interacting with the note list.
 *
 * @property statisticsList statistic items to interact. See [StatisticsItem].
 * @property onItemClick callback to handle the clicked item.
 */
class StatisticsAdapter(
    private val statisticsList: List<StatisticsItem>,
    private val onItemClick: (StatisticsSection) -> Unit
) : RecyclerView.Adapter<StatisticsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTitleBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            holder.bind(statisticsList[position])
        }
    }

    override fun getItemCount(): Int {
        return statisticsList.size
    }

    /**
     * Statistics item [ViewHolder].
     */
    inner class ViewHolder(
        private val binding: ItemTitleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Fill the statistics item view with the information from the [item].
         *
         * @param item information to paste into the statistics item view.
         */
        fun bind(item: StatisticsItem) {
            binding.titleTextView.setText(item.titleResId)
            binding.root.setOnClickListener {
                onItemClick(item.statisticsSection)
            }
        }
    }
}
