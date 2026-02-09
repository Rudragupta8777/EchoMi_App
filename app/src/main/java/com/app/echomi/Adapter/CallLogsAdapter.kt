package com.app.echomi.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.echomi.R
import com.app.echomi.Services.ContactHelper
import com.app.echomi.data.CallLog
import java.util.*

class CallLogsAdapter(
    private var logs: MutableList<CallLog>,
    private val onItemClick: (CallLog) -> Unit,
    private val context: Context
) : RecyclerView.Adapter<CallLogsAdapter.LogViewHolder>(), Filterable {

    private var fullLogsList: List<CallLog> = ArrayList(logs)

    // Updated ViewHolder to match the new "Obsidian" item_call_log.xml
    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val callerName: TextView = view.findViewById(R.id.callerName)
        val callTime: TextView = view.findViewById(R.id.callTime)
        // Note: Summary removed from list view for cleaner "Tech" look
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_call_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]

        // 1. Set Name (Or Number if name not found)
        val displayName = getContactDisplayName(log.callerNumber)
        holder.callerName.text = displayName

        // 2. Set Time with "Tech" formatting
        // Assumes format like "2023-10-27T10:30:00..."
        val timeString = try {
            val time = log.startTime.substring(11, 16) // Extract HH:MM
            "$time • SYSTEM"
        } catch (e: Exception) {
            "UNKNOWN • SYSTEM"
        }
        holder.callTime.text = timeString

        // 3. Click Listener
        holder.itemView.setOnClickListener {
            onItemClick(log)
        }
    }

    override fun getItemCount() = logs.size

    fun updateLogs(newLogs: List<CallLog>) {
        logs.clear()
        logs.addAll(newLogs)
        fullLogsList = ArrayList(newLogs)
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return callLogFilter
    }

    private val callLogFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList = mutableListOf<CallLog>()
            if (constraint.isNullOrEmpty()) {
                filteredList.addAll(fullLogsList)
            } else {
                val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim()
                for (item in fullLogsList) {
                    val displayName = getContactDisplayName(item.callerNumber).lowercase(Locale.getDefault())
                    val originalNumber = item.callerNumber.lowercase(Locale.getDefault())

                    // Filter matches logic
                    if (displayName.contains(filterPattern) ||
                        originalNumber.contains(filterPattern) ||
                        item.summary?.lowercase(Locale.getDefault())?.contains(filterPattern) == true) {
                        filteredList.add(item)
                    }
                }
            }
            val results = FilterResults()
            results.values = filteredList
            return results
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            logs.clear()
            if (results?.values is List<*>) {
                logs.addAll(results.values as List<CallLog>)
            }
            notifyDataSetChanged()
        }
    }

    private fun getContactDisplayName(phoneNumber: String): String {
        return ContactHelper.getContactName(context, phoneNumber) ?: phoneNumber
    }
}