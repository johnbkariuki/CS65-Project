package com.example.checkmate

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream

// for formatting items in the receipt list
class HistoryListAdapter(val context: Context, var historyList: List<ReceiptEntry>) : BaseAdapter(){

    var username = ""  // current user's username

    override fun getItem(position: Int): Any {
        return historyList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return historyList.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = View.inflate(context, R.layout.layout_historylist_adapter,null)

        // get ReceiptEntry object
        val receipt = historyList[position]

        // get TextViews
        val amountPaidText = view.findViewById<TextView>(R.id.amount_paid)
        val receiptTitleText = view.findViewById<TextView>(R.id.receipt_title)
        val receiptDateText = view.findViewById<TextView>(R.id.receipt_date)

        // calculate amount user paid
        var amountPaid = 0.0
        val translatedPayerList = Globals.Byte2ArrayList(receipt.payerList)
        val translatedPriceList = Globals.Byte2ArrayList(receipt.priceList)
        for (i in 0 until translatedPayerList.size) {
            val payer = translatedPayerList[i]
            val price = translatedPriceList[i].toFloat()
            if (payer == username) {
                amountPaid += price
            }
        }

        // set textviews
        amountPaidText.text = "-$amountPaid"
        receiptTitleText.text = receipt.title
        receiptDateText.text = receipt.date

        return view
    }

    fun replace(newHistoryList: List<ReceiptEntry>){
        historyList = newHistoryList
    }
}
