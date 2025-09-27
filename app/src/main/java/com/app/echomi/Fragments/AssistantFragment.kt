package com.app.echomi.Fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.echomi.Adapter.PromptsAdapter
import com.app.echomi.Network.RetrofitInstance
import com.app.echomi.R
import com.app.echomi.data.Prompt
import com.app.echomi.data.UpdatePromptRequest
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class AssistantFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PromptsAdapter
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_assistant, container, false)
        recyclerView = view.findViewById(R.id.promptsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)

        setupRecyclerView()
        fetchPrompts()

        return view
    }

    private fun setupRecyclerView() {
        adapter = PromptsAdapter(emptyList()) { prompt ->
            showEditPromptDialog(prompt)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun fetchPrompts() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getPrompts()
                if (response.isSuccessful) {
                    adapter.updatePrompts(response.body() ?: emptyList())
                } else {
                    Toast.makeText(context, "Failed to load prompts", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showEditPromptDialog(prompt: Prompt) {
        val editText = EditText(requireContext()).apply {
            setText(prompt.instructions)
            // Add some padding for better appearance
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Instructions for ${prompt.promptType.replaceFirstChar { it.uppercase() }}")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newInstructions = editText.text.toString()
                updatePrompt(prompt.promptType, newInstructions)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePrompt(promptType: String, instructions: String) {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val request = UpdatePromptRequest(instructions)
                val response = RetrofitInstance.api.updatePrompt(promptType, request)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Prompt saved!", Toast.LENGTH_SHORT).show()
                    fetchPrompts() // Refresh the list with the updated data
                } else {
                    throw Exception("Failed to update prompt")
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}