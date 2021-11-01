package com.example.checkmate

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class HistoryFragment : Fragment() {
    private lateinit var prefs: SharedPreferences
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mFirebaseUser: FirebaseUser
    private lateinit var mDatabase: DatabaseReference
    private lateinit var mUserId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

//        // load firebase
//        mFirebaseAuth = FirebaseAuth.getInstance()
//        mDatabase = FirebaseDatabase.getInstance().getReference("users")
//
//        // grab the text view and load the curr user's profile
//        prefs = requireActivity().getSharedPreferences(MainActivity.MY_PREFERENCES, Context.MODE_PRIVATE)
//        val emailS = prefs.getString(SignUpActivity.EMAIL_KEY,"")!!
//        val passwordS = prefs.getString(SignUpActivity.PASSWORD_KEY,"")!!
//
//        // login and grab the user
//        mFirebaseAuth.signInWithEmailAndPassword(emailS, passwordS)
//        mFirebaseUser = mFirebaseAuth.currentUser!!
//        mUserId = mFirebaseUser.uid
//
//        // grab the user and user object

        return view
    }

}