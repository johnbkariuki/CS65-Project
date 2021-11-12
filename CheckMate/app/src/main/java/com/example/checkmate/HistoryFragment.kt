package com.example.checkmate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class HistoryFragment : Fragment() {

    // for displaying history list
    private var username = ""  // current user's username
    private var historyList = listOf<ReceiptEntry>()
    private lateinit var listAdapter: HistoryListAdapter
    private lateinit var historyListView: ListView
    private lateinit var headerText: TextView

    // for accessing firebase
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mUserId: String
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

        headerText = view.findViewById(R.id.historyHeader)

        // check if logged in
        val pref = requireActivity().getSharedPreferences(Globals.MY_PREFERENCES, Context.MODE_PRIVATE)
        val loggedIn = pref.getBoolean(Globals.LOGGED_IN_KEY, false)

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

            mFirebaseFirestore.collection("users").document(mUserId).get()
                .addOnSuccessListener {
                    // get username
                    username = it.data!!["username"].toString()
                    headerText.text = "Welcome, @$username! \nHere's a look at your payment history:"

                    // attaching list adapter
                    listAdapter = HistoryListAdapter(requireActivity(), historyList)
                    listAdapter.username = username
                    historyListView = view.findViewById(R.id.historyList)
                    historyListView.adapter = listAdapter

                    // create list
                    /*
                    receiptEntryViewModel.receiptList.observe(requireActivity(), Observer { it ->
                        listAdapter.replace(it)
                        listAdapter.notifyDataSetChanged()
                    })
                     */
            
            mFirebaseFirestore.collection("users").document(mUserId).addSnapshotListener { value, error ->
                val receipts = value!!.data!!["receipts"] as ArrayList<*>
                val historyListFirestore = ArrayList<ReceiptEntry>()
                if (receipts.isNotEmpty()) {
                    for (receipt in receipts) {
                        val receiptObj = receipt as HashMap<*, *>
                        val receiptEntry = ReceiptEntry()
                        receiptEntry.date = receiptObj["date"].toString()
                        receiptEntry.itemList =
                            Globals.ArrayList2Byte(receiptObj["itemList"] as ArrayList<String>)
                        receiptEntry.payer = receiptObj["payer"].toString()
                        receiptEntry.payerList =
                            Globals.ArrayList2Byte(receiptObj["payerList"] as ArrayList<String>)
                        receiptEntry.priceList =
                            Globals.ArrayList2Byte(receiptObj["priceList"] as ArrayList<String>)
                        receiptEntry.title = receiptObj["title"].toString()

                        // add receipt entry to history list firestore
                        historyListFirestore.add(receiptEntry)
                    }
                }

                // notify dataset changed
                listAdapter.replace(historyListFirestore)
                listAdapter.notifyDataSetChanged()
            }

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

                        // launch ReceiptActivity
                        intent.putExtra(Globals.RECEIPT_MODE_KEY, Globals.RECEIPT_HISTORY_MODE)
                        parent.context?.startActivity(intent)
                    }
                }
        }
        return view
    }
}