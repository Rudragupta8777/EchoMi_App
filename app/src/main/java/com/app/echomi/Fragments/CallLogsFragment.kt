package com.app.echomi.Fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var searchEditText: EditText

    companion object {
        private const val CONTACTS_PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_call_logs, container, false)

        recyclerView = view.findViewById(R.id.callLogsRecyclerView)
        loadingAnimationView = view.findViewById(R.id.loadingAnimationView)
        emptyStateContainer = view.findViewById(R.id.emptyStateTextView) // This is the LinearLayout container
        searchEditText = view.findViewById(R.id.searchEditText)

        setupRecyclerView()
        setupSearch()

        checkContactsPermission()

        return view
    }

    private fun setupRecyclerView() {
        adapter = CallLogsAdapter(mutableListOf(), { clickedLog ->
            val intent = Intent(activity, CallDetailScreen::class.java)
            // FIX: Changed from .id to ._id
            intent.putExtra("CALL_ID", clickedLog._id)
            startActivity(intent)
        }, requireContext())

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

    private fun checkContactsPermission() {
        if (context == null) return

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                fetchCallLogs()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                android.Manifest.permission.READ_CONTACTS
            ) -> {
                Toast.makeText(context, "Contact permission needed for names", Toast.LENGTH_LONG).show()
                requestContactsPermission()
            }
            else -> {
                requestContactsPermission()
            }
        }
    }

    private fun requestContactsPermission() {
        requestPermissions(
            arrayOf(android.Manifest.permission.READ_CONTACTS),
            CONTACTS_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CONTACTS_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    fetchCallLogs()
                } else {
                    fetchCallLogs() // Fetch anyway, just without names
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun fetchCallLogs() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getCallLogs()
                if (response.isSuccessful) {
                    val logs = response.body()
                    if (logs.isNullOrEmpty()) {
                        showEmptyState(true)
                    } else {
                        adapter.updateLogs(logs)
                        showEmptyState(false)
                    }
                } else {
                    Toast.makeText(context, "Failed to load logs", Toast.LENGTH_SHORT).show()
                    showEmptyState(true)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState(true)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            loadingAnimationView.visibility = View.VISIBLE
            loadingAnimationView.playAnimation()
            emptyStateContainer.visibility = View.GONE
            recyclerView.visibility = View.GONE
        } else {
            loadingAnimationView.visibility = View.GONE
            loadingAnimationView.cancelAnimation()
        }
    }

    private fun showEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            emptyStateContainer.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateContainer.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        if (::loadingAnimationView.isInitialized && loadingAnimationView.visibility == View.VISIBLE) {
            loadingAnimationView.playAnimation()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::loadingAnimationView.isInitialized) {
            loadingAnimationView.pauseAnimation()
        }
    }
}