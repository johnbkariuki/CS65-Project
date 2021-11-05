package com.example.checkmate

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
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
        val loggedIn = pref.getBoolean(MainActivity.LOGGED_IN_KEY, false)
//        loggedIn = false

        if (loggedIn) {

            val email = pref.getString(SignUpActivity.EMAIL_KEY, "")!!
            val password = pref.getString(SignUpActivity.PASSWORD_KEY, "")!!

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
                    })

                    // check for if user clicks on item in history list
                    historyListView.setOnItemClickListener() { parent: AdapterView<*>, view: View, position: Int, id: Long ->
                        val receiptEntry = listAdapter.getItem(position) as ReceiptEntry
                        println("debug: entry #$position selected")

                        // pass needed parameters to ReceiptActivity intent
                        val intent = Intent(parent.context, ReceiptActivity::class.java)
                        intent.putExtra(Globals.RECEIPT_TITLE_KEY, receiptEntry.title)
                        intent.putExtra(Globals.RECEIPT_PRICELIST_KEY, Globals.Byte2ArrayList(receiptEntry.priceList))
                        intent.putExtra(Globals.RECEIPT_ITEMLIST_KEY, Globals.Byte2ArrayList(receiptEntry.itemList))
                        intent.putExtra(Globals.RECEIPT_PAYERLIST_KEY, Globals.Byte2ArrayList(receiptEntry.payerList))

                        intent.putExtra(Globals.RECEIPT_MODE_KEY, Globals.RECEIPT_HISTORY_MODE)
                        parent.context?.startActivity(intent)
                    }
                }
        }
        return view
    }
}