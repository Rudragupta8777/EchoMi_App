package com.app.echomi.Fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.app.echomi.Adapter.CallLogsAdapter
import com.app.echomi.CallDetailScreen
import com.app.echomi.Network.RetrofitInstance
import com.app.echomi.R
import kotlinx.coroutines.launch

class CallLogsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CallLogsAdapter
    private lateinit var loadingAnimationView: LottieAnimationView
    private lateinit var emptyStateTextView: TextView
    private lateinit var searchEditText: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_call_logs, container, false)

        recyclerView = view.findViewById(R.id.callLogsRecyclerView)
        loadingAnimationView = view.findViewById(R.id.loadingAnimationView)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView)
        searchEditText = view.findViewById(R.id.searchEditText)

        setupRecyclerView()
        setupSearch()
        fetchCallLogs()

        // Configure Lottie animation with error handling
        try {
            loadingAnimationView.speed = 1.0f
            loadingAnimationView.playAnimation() // Ensure animation plays (redundant with autoPlay=true)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load animation: ${e.message}", Toast.LENGTH_LONG).show()
            loadingAnimationView.visibility = View.GONE // Hide animation view on failure
        }

        // Add failure listener for Lottie animation
        loadingAnimationView.addLottieOnCompositionLoadedListener { composition ->
            if (composition == null) {
                Toast.makeText(context, "Failed to load Lottie animation", Toast.LENGTH_LONG).show()
                loadingAnimationView.visibility = View.GONE // Hide animation view on failure
            }
        }

        return view
    }

    private fun setupRecyclerView() {
        adapter = CallLogsAdapter(mutableListOf()) { clickedLog ->
            val intent = Intent(activity, CallDetailScreen::class.java)
            intent.putExtra("CALL_ID", clickedLog._id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun fetchCallLogs() {
        loadingAnimationView.visibility = View.VISIBLE // Show animation during loading
        emptyStateTextView.visibility = View.GONE
        recyclerView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getCallLogs()
                if (response.isSuccessful) {
                    val logs = response.body()
                    if (logs.isNullOrEmpty()) {
                        emptyStateTextView.visibility = View.VISIBLE
                    } else {
                        adapter.updateLogs(logs)
                        recyclerView.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(context, "Failed to load logs", Toast.LENGTH_SHORT).show()
                    emptyStateTextView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                emptyStateTextView.visibility = View.VISIBLE
            } finally {
                loadingAnimationView.visibility = View.GONE // Hide animation after loading
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause animation when fragment is paused
        if (::loadingAnimationView.isInitialized) {
            loadingAnimationView.pauseAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume animation when fragment is resumed
        if (::loadingAnimationView.isInitialized) {
            try {
                loadingAnimationView.playAnimation()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to resume animation: ${e.message}", Toast.LENGTH_LONG).show()
                loadingAnimationView.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel animation when view is destroyed
        if (::loadingAnimationView.isInitialized) {
            loadingAnimationView.cancelAnimation()
        }
    }
}