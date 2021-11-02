package com.example.checkmate

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ProfileFragment : Fragment() {
    // views
    private lateinit var logoutButton: Button
    private lateinit var saveButton: Button
    private lateinit var usernameText: TextView
    private lateinit var emailText: TextView
    private lateinit var userImage: ImageView

    // firebase and shared prefs
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mCurrUser: FirebaseUser
    private lateinit var pref: SharedPreferences

    private var email=""
    private var username=""
    private var password=""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // grab existing email and pswd
        pref = requireActivity().getSharedPreferences(MainActivity.MY_PREFERENCES, Context.MODE_PRIVATE)
        email = pref.getString(SignUpActivity.EMAIL_KEY, "")!!
        password = pref.getString(SignUpActivity.PASSWORD_KEY, "")!!

        // firebase and load the view
        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseAuth.signInWithEmailAndPassword(email, password)
        mCurrUser = mFirebaseAuth.currentUser!!
        loadView(view)

        // set onclick listeners
        // logoutButton
        logoutButton = view.findViewById(R.id.logout_button)
        logoutButton.setOnClickListener {
            val editor: SharedPreferences.Editor = pref.edit()
            editor.putString(SignUpActivity.EMAIL_KEY, "")
            editor.putString(SignUpActivity.PASSWORD_KEY, "")
            editor.putBoolean(MainActivity.LOGGED_IN_KEY,false)
            editor.apply()

            // logout
            mFirebaseAuth.signOut()

            val intent = Intent(requireActivity(), SignInSignUpActivity::class.java)
            startActivity(intent)
        }

        // saveButton
        saveButton = view.findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            val editor: SharedPreferences.Editor = pref.edit()
            editor.putString(SignUpActivity.EMAIL_KEY, email)
            editor.putString(SignUpActivity.PASSWORD_KEY, password)
            editor.putBoolean(MainActivity.LOGGED_IN_KEY,true)
            editor.apply()
        }

        return view
    }

    private fun loadView(view: View) {
        // set email text
        emailText = view.findViewById(R.id.email_profile)
        emailText.setText(email)

        // set username text
        usernameText = view.findViewById(R.id.username_profile)
        //username = mCurrUser.displayName!!
        // set profile image
    }
}