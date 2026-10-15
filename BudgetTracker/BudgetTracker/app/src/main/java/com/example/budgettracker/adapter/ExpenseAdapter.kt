package com.example.budgettracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.data.ExpenseEntry
import com.example.budgettracker.databinding.ItemExpenseBinding

/**
 * RecyclerView adapter for displaying expense entries in a list.
 *
 * Uses ListAdapter with DiffUtil for efficient, animated updates.
 * Calls [onPhotoClick] when the user taps the photo indicator on an entry that has a photo.
 */
class ExpenseAdapter(
    private val onPhotoClick: (ExpenseEntry) -> Unit
) : ListAdapter<ExpenseEntry, ExpenseAdapter.ExpenseViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position), onPhotoClick)
    }

    /** ViewHolder for a single expense entry row. */
    class ExpenseViewHolder(private val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: ExpenseEntry, onPhotoClick: (ExpenseEntry) -> Unit) {
            // Display all fields
            binding.tvDescription.text = entry.description
            binding.tvCategory.text = entry.categoryName
            binding.tvAmount.text = "R${"%.2f".format(entry.amount)}"
            binding.tvDate.text = entry.date
            binding.tvTime.text = "${entry.startTime} – ${entry.endTime}"

            // Show/hide photo icon based on whether a photo exists
            if (entry.photoPath != null) {
                binding.btnViewPhoto.visibility = View.VISIBLE
                binding.btnViewPhoto.setOnClickListener { onPhotoClick(entry) }
            } else {
                binding.btnViewPhoto.visibility = View.GONE
            }
        }
    }

    /** DiffUtil callback for detecting changes between expense entry lists. */
    class DiffCallback : DiffUtil.ItemCallback<ExpenseEntry>() {
        override fun areItemsTheSame(oldItem: ExpenseEntry, newItem: ExpenseEntry) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ExpenseEntry, newItem: ExpenseEntry) =
            oldItem == newItem
    }
}
