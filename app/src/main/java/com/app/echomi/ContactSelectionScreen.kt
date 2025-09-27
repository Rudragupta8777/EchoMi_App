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
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var progressBar: ProgressBar
    private lateinit var searchEditText: EditText
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

        recyclerView = findViewById(R.id.contactsRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        searchEditText = findViewById(R.id.searchEditText)
        val saveButton: Button = findViewById(R.id.saveButton)
        val skipButton: Button = findViewById(R.id.skipButton)

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
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                // Step 1: Fetch saved contacts from backend
                val savedContactsResponse = RetrofitInstance.api.getSavedContacts()
                val savedContacts = if (savedContactsResponse.isSuccessful)
                    savedContactsResponse.body() ?: emptyList()
                else
                    emptyList()

                // Create a map for quick lookup by phone number (normalized)
                val savedContactsMap = savedContacts.associateBy {
                    normalizePhoneNumber(it.phoneNumber)
                }

                // Step 2: Load phone contacts
                val phoneContacts = loadContactsFromPhone()

                // Step 3: Merge backend saved contacts with phone contacts (remove duplicates)
                val mergedContacts = mutableListOf<Contact>()
                val processedPhoneNumbers = mutableSetOf<String>()

                phoneContacts.forEach { contact ->
                    val normalizedNumber = normalizePhoneNumber(contact.phoneNumber)

                    // Check if we've already processed this number (avoid duplicates)
                    if (processedPhoneNumbers.add(normalizedNumber)) {
                        // Check if this contact exists in saved contacts
                        val savedContact = savedContactsMap[normalizedNumber]
                        if (savedContact != null) {
                            // Use the saved contact's role
                            contact.role = savedContact.role ?: "" // Handle null role
                        }
                        mergedContacts.add(contact)
                    }
                }

                // Step 4: Update UI
                withContext(Dispatchers.Main) {
                    fullContactsList.clear()
                    fullContactsList.addAll(mergedContacts)
                    sortAndFilterList()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ContactSelectionScreen, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun deduplicateContacts(
        phoneContacts: List<Contact>,
        savedContactsMap: Map<String, CategorizedContact>
    ): List<Contact> {
        val mergedContacts = mutableListOf<Contact>()
        val processedNumbers = mutableSetOf<String>()
        val processedNames = mutableSetOf<String>()

        phoneContacts.forEach { contact ->
            val normalizedNumber = normalizePhoneNumber(contact.phoneNumber)

            // Check for duplicates by both number and name
            val isDuplicateByNumber = normalizedNumber in processedNumbers
            val isDuplicateByName = contact.name in processedNames

            if (!isDuplicateByNumber && !isDuplicateByName) {
                // Apply saved role if exists
                val savedContact = savedContactsMap[normalizedNumber]
                if (savedContact != null) {
                    contact.role = savedContact.role
                }

                mergedContacts.add(contact)
                processedNumbers.add(normalizedNumber)
                processedNames.add(contact.name)
            }
        }

        return mergedContacts
    }

    // Enhanced normalization function
    private fun normalizePhoneNumber(phoneNumber: String): String {
        var normalized = phoneNumber.replace("[^0-9+]".toRegex(), "")

        // Remove country code if it's a US number for better matching
        if (normalized.startsWith("+1")) {
            normalized = normalized.substring(2)
        } else if (normalized.startsWith("1") && normalized.length == 11) {
            normalized = normalized.substring(1)
        }

        // Take only last 10 digits (standard US number)
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

                // Avoid adding duplicates from phone contacts itself
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

        progressBar.visibility = View.VISIBLE
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
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun navigateToMainApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}