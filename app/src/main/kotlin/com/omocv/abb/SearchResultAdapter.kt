package com.omocv.abb

import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for displaying search results with clickable items
 */
class SearchResultAdapter(
    private val results: List<SearchResult>,
    private val searchQuery: String,
    private val onItemClick: (SearchResult) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder>() {

    data class SearchResult(
        val lineNumber: Int,
        val lineContent: String,
        val startIndex: Int,
        val endIndex: Int
    )

    class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLineNumber: TextView = itemView.findViewById(R.id.tvLineNumber)
        val tvLineContent: TextView = itemView.findViewById(R.id.tvLineContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return SearchResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val result = results[position]
        
        holder.tvLineNumber.text = holder.itemView.context.getString(
            R.string.line_number,
            result.lineNumber.toString()
        )
        
        // Highlight the search query in the line content
        val spannable = SpannableString(result.lineContent)
        val startIdx = result.lineContent.indexOf(searchQuery, ignoreCase = true)
        if (startIdx >= 0) {
            spannable.setSpan(
                BackgroundColorSpan(HighlightColors.getErrorHighlightColor(holder.itemView.context)),
                startIdx,
                startIdx + searchQuery.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        holder.tvLineContent.text = spannable
        
        holder.itemView.setOnClickListener {
            onItemClick(result)
        }
    }

    override fun getItemCount(): Int = results.size
}
