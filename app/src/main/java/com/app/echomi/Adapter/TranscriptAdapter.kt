package com.app.echomi.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.echomi.R
import com.app.echomi.data.TranscriptMessage
import java.text.SimpleDateFormat
import java.util.*

class TranscriptAdapter : RecyclerView.Adapter<TranscriptAdapter.TranscriptViewHolder>() {

    private var transcript: List<TranscriptMessage> = emptyList()

    companion object {
        private const val VIEW_TYPE_CALLER = 1
        private const val VIEW_TYPE_AI = 2
    }

    class TranscriptViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val speakerText: TextView = view.findViewById(R.id.speakerTextView)
        val messageText: TextView = view.findViewById(R.id.messageTextView)
        val timestampText: TextView = view.findViewById(R.id.timestampTextView)
    }

    override fun getItemViewType(position: Int): Int {
        val speaker = transcript[position].speaker.lowercase(Locale.getDefault())
        return if (speaker.contains("ai") || speaker.contains("assistant")) {
            VIEW_TYPE_AI
        } else {
            VIEW_TYPE_CALLER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TranscriptViewHolder {
        // Use the new layout files from the Obsidian theme
        val layoutRes = if (viewType == VIEW_TYPE_AI) {
            R.layout.item_transcript_message_right
        } else {
            R.layout.item_transcript_message_left
        }

        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return TranscriptViewHolder(view)
    }

    override fun onBindViewHolder(holder: TranscriptViewHolder, position: Int) {
        val message = transcript[position]
        val context = holder.itemView.context

        // Set Speaker Name & Color based on type
        if (getItemViewType(position) == VIEW_TYPE_AI) {
            holder.speakerText.text = "AI ASSISTANT"
            // Use the new emerald green for AI
            holder.speakerText.setTextColor(ContextCompat.getColor(context, R.color.terminal_green))
        } else {
            holder.speakerText.text = "INCOMING CALLER"
            // Use secondary text color for Caller
            holder.speakerText.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }

        holder.messageText.text = message.text

        // Format timestamp for the "Tech" look (e.g., "14:30")
        try {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).parse(message.timestamp)
            val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date ?: Date())
            holder.timestampText.text = formattedTime
        } catch (e: Exception) {
            holder.timestampText.text = "00:00"
        }
    }

    override fun getItemCount() = transcript.size

    fun updateTranscript(newTranscript: List<TranscriptMessage>) {
        transcript = newTranscript
        notifyDataSetChanged()
    }
}