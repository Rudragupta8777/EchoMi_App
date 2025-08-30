package com.echomi.app

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.echomi.app.Adapter.ContactsAdapter
import com.echomi.app.data.CategorizedContact
import com.echomi.app.data.Contact
import com.echomi.app.data.ContactRequest
import com.echomi.app.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.collections.sortWith
import kotlin.collections.toSet
import kotlin.comparisons.thenBy
import kotlin.io.use
import kotlin.jvm.java

class ContactSelectionActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_contact_selection)

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
                val savedContacts = if (savedContactsResponse.isSuccessful) savedContactsResponse.body() ?: emptyList() else emptyList()
                val savedPhoneNumbers = savedContacts.map { it.phoneNumber }.toSet()

                // Step 2: Fetch all contacts from the phone
                val phoneContacts = loadContactsFromPhone()

                // Step 3: Merge the two lists
                phoneContacts.forEach { contact ->
                    if (contact.phoneNumber in savedPhoneNumbers) {
                        contact.role = "family"
                    }
                }

                // Step 4: Update UI on the main thread
                withContext(Dispatchers.Main) {
                    fullContactsList.clear()
                    fullContactsList.addAll(phoneContacts)
                    sortAndFilterList() // Sort and display the merged list
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ContactSelectionActivity, "Error loading contacts: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private suspend fun loadContactsFromPhone(): List<Contact> = withContext(Dispatchers.IO) {
        val tempContactList = mutableListOf<Contact>()
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
                val name = it.getString(nameIndex)
                val number = it.getString(numberIndex)
                tempContactList.add(Contact(id, name, number))
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
                    Toast.makeText(this@ContactSelectionActivity, "Contacts updated!", Toast.LENGTH_SHORT).show()
                    navigateToMainApp()
                } else {
                    throw Exception("Failed to save contacts: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Toast.makeText(this@ContactSelectionActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
