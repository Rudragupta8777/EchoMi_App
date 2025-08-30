package com.echomi.app.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.echomi.app.R
import com.echomi.app.data.Contact
import java.util.Locale
import kotlin.collections.filter
import kotlin.collections.find

class ContactsAdapter(
    private var displayedContacts: MutableList<Contact>,
    private val fullContactList: List<Contact>, // A reference to the complete list
    private val onRoleChanged: () -> Unit // A callback to notify the Activity to re-sort
) :
    RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>(), Filterable {

    class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.contactNameTextView)
        val numberTextView: TextView = view.findViewById(R.id.contactNumberTextView)
        val familyCheckBox: CheckBox = view.findViewById(R.id.familyCheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = displayedContacts[position]
        holder.nameTextView.text = contact.name
        holder.numberTextView.text = contact.phoneNumber

        holder.familyCheckBox.setOnCheckedChangeListener(null)
        holder.familyCheckBox.isChecked = contact.role == "family"

        holder.familyCheckBox.setOnCheckedChangeListener { _, isChecked ->
            val originalContact = fullContactList.find { it.id == contact.id }
            originalContact?.role = if (isChecked) "family" else "default"
            // Notify the activity that a change occurred so it can re-sort and re-filter
            onRoleChanged()
        }
    }

    override fun getItemCount() = displayedContacts.size

    fun getCategorizedContacts(): List<Contact> {
        return fullContactList.filter { it.role == "family" }
    }

    override fun getFilter(): Filter {
        return contactFilter
    }

    private val contactFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList = mutableListOf<Contact>()
            val sourceList = ArrayList(fullContactList) // Use a copy of the full list

            if (constraint.isNullOrEmpty()) {
                filteredList.addAll(sourceList)
            } else {
                val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim()
                for (item in sourceList) {
                    if (item.name.lowercase(Locale.getDefault()).contains(filterPattern)) {
                        filteredList.add(item)
                    }
                }
            }
            val results = FilterResults()
            results.values = filteredList
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            displayedContacts.clear()
            if (results?.values is List<*>) {
                displayedContacts.addAll(results.values as List<Contact>)
            }
            notifyDataSetChanged()
        }
    }
}

