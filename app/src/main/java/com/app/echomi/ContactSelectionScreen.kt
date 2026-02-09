package com.app.echomi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.app.echomi.Adapter.ContactsAdapter
import com.app.echomi.Network.RetrofitInstance
import com.app.echomi.data.CategorizedContact
import com.app.echomi.data.Contact
import com.app.echomi.data.ContactRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactSelectionScreen : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var loadingAnimationView: LottieAnimationView
    private lateinit var searchEditText: EditText

    // Updated Views for New UI
    private lateinit var headerLayout: LinearLayout
    private lateinit var searchContainer: LinearLayout
    private lateinit var actionFooter: LinearLayout

    private val fullContactsList = mutableListOf<Contact>()
    private val displayedContactsList = mutableListOf<Contact>()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                loadAndMergeContacts()
            } else {
                Toast.makeText(this, "Permission denied. Cannot load contacts.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_contact_selection_screen)

        // Initialize views (Mapped to new XML IDs)
        headerLayout = findViewById(R.id.headerLayout)
        searchContainer = findViewById(R.id.searchContainer) // Changed from search_card
        actionFooter = findViewById(R.id.actionFooter)       // Changed from buttonContainer

        recyclerView = findViewById(R.id.contactsRecyclerView)
        loadingAnimationView = findViewById(R.id.loadingAnimationView)
        searchEditText = findViewById(R.id.searchEditText)

        val saveButton: Button = findViewById(R.id.saveButton)
        val skipButton: Button = findViewById(R.id.skipButton)

        // Configure Lottie
        try {
            loadingAnimationView.speed = 1.0f
        } catch (e: Exception) {
            loadingAnimationView.visibility = View.GONE
        }

        setupRecyclerView()
        setupSearch()
        checkAndLoadContacts()

        saveButton.setOnClickListener {
            saveCategorizedContacts()
        }
        skipButton.setOnClickListener {
            navigateToMainApp()
        }
    }

    private fun setupRecyclerView() {
        contactsAdapter = ContactsAdapter(displayedContactsList, fullContactsList) {
            sortAndFilterList()
        }
        recyclerView.adapter = contactsAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                contactsAdapter.filter.filter(s)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun checkAndLoadContacts() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                loadAndMergeContacts()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun loadAndMergeContacts() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                // Step 1: Fetch saved contacts from backend
                val savedContactsResponse = RetrofitInstance.api.getSavedContacts()
                val savedContacts = if (savedContactsResponse.isSuccessful)
                    savedContactsResponse.body() ?: emptyList()
                else
                    emptyList()

                // Map for quick lookup
                val savedContactsMap = savedContacts.associateBy {
                    normalizePhoneNumber(it.phoneNumber)
                }

                // Step 2: Load phone contacts
                val phoneContacts = loadContactsFromPhone()

                // Step 3: Merge
                val mergedContacts = mutableListOf<Contact>()
                val processedPhoneNumbers = mutableSetOf<String>()

                phoneContacts.forEach { contact ->
                    val normalizedNumber = normalizePhoneNumber(contact.phoneNumber)

                    if (processedPhoneNumbers.add(normalizedNumber)) {
                        val savedContact = savedContactsMap[normalizedNumber]
                        if (savedContact != null) {
                            contact.role = savedContact.role ?: ""
                        }
                        mergedContacts.add(contact)
                    }
                }

                // Step 4: Update UI
                withContext(Dispatchers.Main) {
                    fullContactsList.clear()
                    fullContactsList.addAll(mergedContacts)
                    sortAndFilterList()
                    showLoading(false)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ContactSelectionScreen, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    showLoading(false)
                }
            }
        }
    }

    private fun normalizePhoneNumber(phoneNumber: String): String {
        var normalized = phoneNumber.replace("[^0-9+]".toRegex(), "")

        if (normalized.startsWith("+1")) {
            normalized = normalized.substring(2)
        } else if (normalized.startsWith("1") && normalized.length == 11) {
            normalized = normalized.substring(1)
        }

        if (normalized.length > 10) {
            normalized = normalized.substring(normalized.length - 10)
        }

        return normalized
    }

    private suspend fun loadContactsFromPhone(): List<Contact> = withContext(Dispatchers.IO) {
        val tempContactList = mutableListOf<Contact>()
        val processedNumbers = mutableSetOf<String>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection, null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val name = it.getString(nameIndex) ?: "Unknown"
                val number = it.getString(numberIndex) ?: ""

                val normalizedNumber = normalizePhoneNumber(number)

                if (normalizedNumber.isNotEmpty() && processedNumbers.add(normalizedNumber)) {
                    tempContactList.add(Contact(id, name, number))
                }
            }
        }
        return@withContext tempContactList
    }

    private fun sortAndFilterList() {
        fullContactsList.sortWith(compareByDescending<Contact> { it.role == "family" }.thenBy { it.name })
        contactsAdapter.filter.filter(searchEditText.text)
    }

    private fun saveCategorizedContacts() {
        val categorizedContacts = contactsAdapter.getCategorizedContacts()
        showLoading(true)
        lifecycleScope.launch {
            try {
                val requestBody = ContactRequest(
                    contacts = categorizedContacts.map {
                        CategorizedContact(it.name, it.phoneNumber, it.role)
                    }
                )

                val response = RetrofitInstance.api.saveContacts(requestBody)

                if (response.isSuccessful) {
                    Toast.makeText(this@ContactSelectionScreen, "Contacts updated!", Toast.LENGTH_SHORT).show()
                    navigateToMainApp()
                } else {
                    throw Exception("Failed to save contacts: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Toast.makeText(this@ContactSelectionScreen, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun navigateToMainApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    // Updated Loading Logic to handle new Views
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            loadingAnimationView.visibility = View.VISIBLE
            loadingAnimationView.playAnimation()

            // Hide Content
            headerLayout.visibility = View.GONE
            searchContainer.visibility = View.GONE
            recyclerView.visibility = View.GONE
            actionFooter.visibility = View.GONE
        } else {
            loadingAnimationView.visibility = View.GONE
            loadingAnimationView.cancelAnimation()

            // Show Content
            headerLayout.visibility = View.VISIBLE
            searchContainer.visibility = View.VISIBLE
            recyclerView.visibility = View.VISIBLE
            actionFooter.visibility = View.VISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        if (::loadingAnimationView.isInitialized) {
            loadingAnimationView.pauseAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::loadingAnimationView.isInitialized && loadingAnimationView.visibility == View.VISIBLE) {
            loadingAnimationView.playAnimation()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::loadingAnimationView.isInitialized) {
            loadingAnimationView.cancelAnimation()
        }
    }
}