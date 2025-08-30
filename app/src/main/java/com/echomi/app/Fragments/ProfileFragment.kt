package com.echomi.app.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.echomi.app.ContactSelectionActivity
import com.echomi.app.LoginActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.echomi.app.R
import kotlin.jvm.java

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val emailTextView: TextView = view.findViewById(R.id.emailTextView)
        val manageContactsButton: Button = view.findViewById(R.id.manageContactsButton)
        val logoutButton: Button = view.findViewById(R.id.logoutButton)

        val currentUser = Firebase.auth.currentUser
        nameTextView.text = currentUser?.displayName ?: "No Name"
        emailTextView.text = currentUser?.email ?: "No Email"

        manageContactsButton.setOnClickListener {
            startActivity(Intent(activity, ContactSelectionActivity::class.java))
        }

        logoutButton.setOnClickListener {
            Firebase.auth.signOut()
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }
}
