package com.example.checkmate

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class UserRetrievedActivity : AppCompatActivity() {
    // for displaying history list
    private var username = ""
    private var phone = ""
    private var historyList = listOf<ReceiptEntry>()
    private lateinit var listAdapter: HistoryListAdapter
    private lateinit var historyListView: ListView
    private lateinit var headerText: TextView
    private lateinit var profileImage: ImageView
    private lateinit var usernameText: TextView
    private lateinit var phoneText: TextView

    // for accessing firebase
    private lateinit var mUserId: String
    private lateinit var mFirebaseFirestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_retrieved)

        val bundle = intent.getBundleExtra("bundle")!!
        username = bundle.getString("key", "")

        headerText = findViewById(R.id.historyHeader)
        profileImage = findViewById(R.id.profile_image)
        usernameText = findViewById(R.id.username_text)
        phoneText = findViewById(R.id.phone_text)
        historyListView = findViewById(R.id.historyList)

        mFirebaseFirestore = FirebaseFirestore.getInstance()
        mFirebaseFirestore.collection("users").get().addOnSuccessListener { all_data ->
            mFirebaseFirestore.collection("users").whereEqualTo("username", username).get()
                .addOnCompleteListener {
                    for (value in it.result.documents) {
                        mUserId = value.id

                        FirebaseStorage.getInstance().reference.child("users/$mUserId").downloadUrl.addOnSuccessListener {
                            Glide.with(this).load(it)
                                .signature(ObjectKey(System.currentTimeMillis().toString()))
                                .into(profileImage)
                        }
                            .addOnFailureListener {
                                Glide.with(this).load(R.drawable.default_image)
                                    .signature(ObjectKey(System.currentTimeMillis().toString()))
                                    .into(profileImage)
                            }

                        username = value.data!!["username"].toString()
                        phone = value.data!!["phone"].toString()

                        usernameText.text = "Username: @$username"
                        phoneText.text = "Phone: $phone"
                        headerText.text = "@$username's payment history:"

                        listAdapter = HistoryListAdapter(this, historyList)
                        listAdapter.username = username
                        historyListView.adapter = listAdapter
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

                                // notify dataset changed
                                listAdapter.replace(historyListFirestore)
                                listAdapter.notifyDataSetChanged()
                            }
                        }

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
                }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.user_lookup_result, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.back -> {
                finish()
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}
