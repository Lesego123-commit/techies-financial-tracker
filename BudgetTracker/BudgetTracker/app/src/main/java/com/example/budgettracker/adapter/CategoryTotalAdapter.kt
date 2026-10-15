package com.example.budgettracker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.data.CategoryTotal
import com.example.budgettracker.databinding.ItemCategoryTotalBinding

/**
 * RecyclerView adapter for displaying category spending totals.
 *
 * Uses ListAdapter with DiffUtil for efficient updates.
 * Each row shows the category name and its total expenditure for the period.
 */
class CategoryTotalAdapter : ListAdapter<CategoryTotal, CategoryTotalAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryTotalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemCategoryTotalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryTotal) {
            binding.tvCategoryName.text = item.categoryName
            binding.tvCategoryTotal.text = "R${"%.2f".format(item.total)}"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CategoryTotal>() {
        override fun areItemsTheSame(oldItem: CategoryTotal, newItem: CategoryTotal) =
            oldItem.categoryName == newItem.categoryName

        override fun areContentsTheSame(oldItem: CategoryTotal, newItem: CategoryTotal) =
            oldItem == newItem
    }
}
