package com.example.checkmate

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore

class HistoryFragment : Fragment() {

    private var username = ""  // current user's username
    private var historyList = listOf<ReceiptEntry>()
    private lateinit var listAdapter: HistoryListAdapter
    private lateinit var historyListView: ListView

    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mFirebaseUser: FirebaseUser
    private lateinit var mUserId: String
    private lateinit var mDatabase: DatabaseReference
    private lateinit var mFirebaseFirestore: FirebaseFirestore
    private lateinit var mCurrUser: FirebaseUser

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        // set up database and view model
        val database = ReceiptEntryDatabase.getInstance(requireActivity())
        val databaseDao = database.receiptEntryDatabaseDao
        val viewModelFactory = ReceiptEntryViewModelFactory(databaseDao)
        val receiptEntryViewModel = ViewModelProvider(this, viewModelFactory).get(ReceiptEntryViewModel::class.java)

        val pref = requireActivity().getSharedPreferences(MainActivity.MY_PREFERENCES, Context.MODE_PRIVATE)
        var loggedIn = pref.getBoolean(MainActivity.LOGGED_IN_KEY, false)
//        loggedIn = false

        if (loggedIn) {

            var email = pref.getString(SignUpActivity.EMAIL_KEY, "")!!
            var password = pref.getString(SignUpActivity.PASSWORD_KEY, "")!!

            // firebase and load the view
            mFirebaseAuth = FirebaseAuth.getInstance()
            mFirebaseFirestore = FirebaseFirestore.getInstance()
            mFirebaseAuth.signInWithEmailAndPassword(email, password)

            if (mFirebaseAuth.currentUser != null) {
                mCurrUser = mFirebaseAuth.currentUser!!
                mUserId = mCurrUser.uid
            }

            // get username
            mFirebaseFirestore.collection("users").document(mUserId).get()
                .addOnSuccessListener {
                    // if logged in
                    username = it.data!!["username"].toString()

                    // creating checklist to format receipt
                    listAdapter = HistoryListAdapter(requireActivity(), historyList)
                    listAdapter.username = username
                    historyListView = view.findViewById(R.id.historyList)
                    historyListView.adapter = listAdapter

                    receiptEntryViewModel.receiptList.observe(requireActivity(), Observer { it ->
                        listAdapter.replace(it)
                        listAdapter.notifyDataSetChanged()
                        // reload list with new popup displays
                        println("debug: len(historyList) = ${it.size}")
                        println("debug: username = $username")
                    })
                }
        }

        return view
    }
}