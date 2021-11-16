package com.example.checkmate

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class HistoryFragment : Fragment() {

    // for displaying history list
    private var username = ""  // current user's username
    private var historyList = listOf<ReceiptEntry>()
    private lateinit var listAdapter: HistoryListAdapter
    private lateinit var historyListView: ListView
    private lateinit var headerText: TextView
    private lateinit var profileImage: ImageView
    private lateinit var usernameText: TextView
    private lateinit var phoneText: TextView

    // for accessing firebase
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mUserId: String
    private lateinit var mFirebaseFirestore: FirebaseFirestore
    private lateinit var mCurrUser: FirebaseUser

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        if (activity == null){
            return view
        }

        // get views
        headerText = view.findViewById(R.id.historyHeader)
        profileImage = view.findViewById(R.id.profile_image)
        usernameText = view.findViewById(R.id.username_text)
        phoneText = view.findViewById(R.id.phone_text)

        // check if logged in
        val pref = requireActivity().getSharedPreferences(Globals.MY_PREFERENCES, Context.MODE_PRIVATE)
        val loggedIn = pref.getBoolean(Globals.LOGGED_IN_KEY, false)

        if (loggedIn){

            val email = pref.getString(SignUpActivity.EMAIL_KEY, "")!!
            val password = pref.getString(SignUpActivity.PASSWORD_KEY, "")!!

            // firebase and load the view
            mFirebaseAuth = FirebaseAuth.getInstance()
            mFirebaseFirestore = FirebaseFirestore.getInstance()
            mFirebaseAuth.signInWithEmailAndPassword(email, password)

            // retrieve userid for firebase
            if (mFirebaseAuth.currentUser != null) {
                mCurrUser = mFirebaseAuth.currentUser!!
                mUserId = mCurrUser.uid
            }

            mFirebaseFirestore.collection("users").get().addOnSuccessListener { all_data ->

                mFirebaseFirestore.collection("users").document(mUserId).get()
                    .addOnSuccessListener {

                        // get username
                        username = it.data!!["username"].toString()
                        val phone = it.data!!["phone"].toString()
                        usernameText.text = "Username: @$username"
                        phoneText.text = "Phone: $phone"
                        headerText.text =
                            "Welcome, @$username! \nHere's a look at your payment history:"

                        // attaching list adapter
                        val historyListFirestore = ArrayList<ReceiptEntry>()
                        listAdapter = HistoryListAdapter(requireActivity(), historyListFirestore)
                        listAdapter.username = username
                        historyListView = view.findViewById(R.id.historyList)
                        historyListView.adapter = listAdapter

                        // check for if user clicks on item in history list
                        historyListView.setOnItemClickListener() { parent: AdapterView<*>, view: View, position: Int, id: Long ->
                            val receiptEntry = listAdapter.getItem(position) as ReceiptEntry

                            // pass needed parameters to ReceiptActivity intent
                            val intent = Intent(parent.context, ReceiptActivity::class.java)
                            intent.putExtra(Globals.RECEIPT_TITLE_KEY, receiptEntry.title)
                            intent.putExtra(
                                Globals.RECEIPT_PRICELIST_KEY,
                                Globals.Byte2ArrayList(receiptEntry.priceList)
                            )
                            intent.putExtra(
                                Globals.RECEIPT_ITEMLIST_KEY,
                                Globals.Byte2ArrayList(receiptEntry.itemList)
                            )
                            intent.putExtra(
                                Globals.RECEIPT_PAYERLIST_KEY,
                                Globals.Byte2ArrayList(receiptEntry.payerList)
                            )

                            // launch ReceiptActivity
                            intent.putExtra(Globals.RECEIPT_MODE_KEY, Globals.RECEIPT_HISTORY_MODE)
                            parent.context?.startActivity(intent)
                        }

                        // load profile pic
                        FirebaseStorage.getInstance().reference.child("users/$mUserId").downloadUrl.addOnSuccessListener {
                            activity?.applicationContext?.let { it1 -> Glide.with(it1).load(it).signature(ObjectKey(System.currentTimeMillis().toString())).into(profileImage) }
                        }
                            .addOnFailureListener {
                                activity?.applicationContext?.let { it1 -> Glide.with(it1).load(R.drawable.default_image).signature(ObjectKey(System.currentTimeMillis().toString())).into(profileImage) }
                            }

                        mFirebaseFirestore.collection("users").document(mUserId)
                            .addSnapshotListener { value, error ->
                                val receipts = value!!.data!!["receipts"] as ArrayList<*>
                                val historyListFirestore = ArrayList<ReceiptEntry>()
                                // load receipts in history
                                if (receipts.isNotEmpty()) {
                                    for (receipt in receipts) {
                                        val receiptObj = receipt as HashMap<*, *>
                                        val receiptEntry = ReceiptEntry()
                                        receiptEntry.date = receiptObj["date"].toString()
                                        receiptEntry.itemList =
                                            Globals.ArrayList2Byte(receiptObj["itemList"] as ArrayList<String>)

                                        var payer = ""
                                        val payer_by_id = receiptObj["payer"].toString()

                                        val payersList_by_id =
                                            receiptObj["payerList"] as ArrayList<String>
                                        val payersList = Array<String>(payersList_by_id.size) { "" }
                                        val mutable_payersList = payersList.toMutableList()

                                        // retrieve payer usernames
                                        for (document in all_data.documents) {
                                            if (payer_by_id == document.id) {
                                                payer = document.data?.get("username") as String
                                            }

                                            for (index in 0 until payersList_by_id.size) {
                                                if (payersList_by_id[index] == document.id) {
                                                    mutable_payersList[index] =
                                                        document.data?.get("username") as String
                                                }
                                            }
                                        }

                                        receiptEntry.payer = payer
                                        receiptEntry.payerList =
                                            Globals.ArrayList2Byte(mutable_payersList as ArrayList<String>)
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
                    }
            }
        }
        return view
    }

    override fun onResume() {
        super.onResume()

        if (this::mFirebaseFirestore.isInitialized) {

            if (activity == null){
                return
            }

            mFirebaseFirestore.collection("users").get().addOnSuccessListener { all_data ->

                mFirebaseFirestore.collection("users").document(mUserId).get()
                    .addOnSuccessListener {
                        // get username
                        username = it.data!!["username"].toString()
                        val phone = it.data!!["phone"].toString()
                        usernameText.text = "Username: @$username"
                        phoneText.text = "Phone: $phone"
                        headerText.text =
                            "Welcome, @$username! \nHere's a look at your payment history:"

                        // attaching list adapter
                        listAdapter.username = username
                        historyListView.adapter = listAdapter

                        // check for if user clicks on item in history list
                        historyListView.setOnItemClickListener() { parent: AdapterView<*>, view: View, position: Int, id: Long ->
                            val receiptEntry = listAdapter.getItem(position) as ReceiptEntry

                            // pass needed parameters to ReceiptActivity intent
                            val intent = Intent(parent.context, ReceiptActivity::class.java)
                            intent.putExtra(Globals.RECEIPT_TITLE_KEY, receiptEntry.title)
                            intent.putExtra(
                                Globals.RECEIPT_PRICELIST_KEY,
                                Globals.Byte2ArrayList(receiptEntry.priceList)
                            )
                            intent.putExtra(
                                Globals.RECEIPT_ITEMLIST_KEY,
                                Globals.Byte2ArrayList(receiptEntry.itemList)
                            )
                            intent.putExtra(
                                Globals.RECEIPT_PAYERLIST_KEY,
                                Globals.Byte2ArrayList(receiptEntry.payerList)
                            )

                            // launch ReceiptActivity
                            intent.putExtra(Globals.RECEIPT_MODE_KEY, Globals.RECEIPT_HISTORY_MODE)
                            parent.context?.startActivity(intent)
                        }

                        FirebaseStorage.getInstance().reference.child("users/$mUserId").downloadUrl.addOnSuccessListener {
                            activity?.applicationContext?.let { it1 -> Glide.with(it1).load(it).signature(ObjectKey(System.currentTimeMillis().toString())).into(profileImage) }
                        }
                            .addOnFailureListener {
                                activity?.applicationContext?.let { it1 -> Glide.with(it1).load(R.drawable.default_image).signature(ObjectKey(System.currentTimeMillis().toString())).into(profileImage) }
                            }

                        mFirebaseFirestore.collection("users").document(mUserId)
                            .addSnapshotListener { value, error ->
                                val receipts = value!!.data!!["receipts"] as ArrayList<*>
                                val historyListFirestore = ArrayList<ReceiptEntry>()
                                if (receipts.isNotEmpty()) {
                                    for (receipt in receipts) {
                                        val receiptObj = receipt as HashMap<*, *>
                                        val receiptEntry = ReceiptEntry()
                                        receiptEntry.date = receiptObj["date"].toString()
                                        receiptEntry.itemList =
                                            Globals.ArrayList2Byte(receiptObj["itemList"] as ArrayList<String>)


                                        var payer = ""
                                        val payer_by_id = receiptObj["payer"].toString()

                                        val payersList_by_id =
                                            receiptObj["payerList"] as ArrayList<String>
                                        val payersList = Array<String>(payersList_by_id.size) { "" }
                                        val mutable_payersList = payersList.toMutableList()

                                        for (document in all_data.documents) {
                                            if (payer_by_id == document.id) {
                                                payer = document.data?.get("username") as String
                                            }

                                            for (index in 0 until payersList_by_id.size) {
                                                if (payersList_by_id[index] == document.id) {
                                                    mutable_payersList[index] =
                                                        document.data?.get("username") as String
                                                }
                                            }
                                        }

                                        receiptEntry.payer = payer
                                        receiptEntry.payerList =
                                            Globals.ArrayList2Byte(mutable_payersList as ArrayList<String>)
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
                    }
            }
        }
    }
}