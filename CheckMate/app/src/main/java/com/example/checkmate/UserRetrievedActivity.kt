package com.example.checkmate

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class UserRetrievedActivity : AppCompatActivity() {
    // for displaying history list
    private var username = ""
    private var venmo = ""
    private var historyList = listOf<ReceiptEntry>()
    private lateinit var listAdapter: HistoryListAdapter
    private lateinit var historyListView: ListView
    private lateinit var headerText: TextView
    private lateinit var profileImage: ImageView
    private lateinit var usernameText: TextView
    private lateinit var venmoText: TextView

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
        venmoText = findViewById(R.id.venmo_text)
        historyListView = findViewById(R.id.historyList)

        mFirebaseFirestore = FirebaseFirestore.getInstance()
        mFirebaseFirestore.collection("users").whereEqualTo("username", username).get()
            .addOnCompleteListener {
                for (value in it.result.documents) {
                    mUserId = value.id

                    FirebaseStorage.getInstance().reference.child("users/$mUserId").downloadUrl.addOnSuccessListener {
                        Glide.with(this).load(it).into(profileImage)
                    }
                        .addOnFailureListener { Glide.with(this).load(R.drawable.default_image).into(profileImage) }

                    username = value.data!!["username"].toString()
                    venmo = value.data!!["venmo"].toString()

                    usernameText.text = "Username: @$username"
                    venmoText.text = "Venmo: @$venmo"
                    headerText.text = "@$username's payment history:"
                }

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

                listAdapter = HistoryListAdapter(this, historyList)
                listAdapter.username = username
                historyListView.adapter = listAdapter
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
