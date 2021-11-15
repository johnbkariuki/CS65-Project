package com.example.checkmate

import android.content.Context
import android.graphics.Color
import android.text.Html
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
import java.lang.NumberFormatException
import android.widget.TextView

import android.text.Spannable

import android.text.style.ForegroundColorSpan

import android.text.SpannableString
import android.text.Spanned
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage


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

        // get TextViews, icons
        val historyIcon = view.findViewById<ImageView>(R.id.history_icon)
        val amountPaidText = view.findViewById<TextView>(R.id.amount_paid)
        val receiptTitleText = view.findViewById<TextView>(R.id.receipt_title)
        val receiptDateText = view.findViewById<TextView>(R.id.receipt_date)

        // calculate amount user paid
        var amountPaid = 0.0
        // total bill
        var totalPaid = 0.0
        val requestor = receipt.payer
        val translatedPayerList = Globals.Byte2ArrayList(receipt.payerList)
        val translatedPriceList = Globals.Byte2ArrayList(receipt.priceList)

        println("debug:$translatedPayerList")
        println("debug:$translatedPriceList")

        try {
            // calculate amount paid by user and total bill cost
            for (i in 0 until translatedPayerList.size) {
                val payer = translatedPayerList[i]
                val price = translatedPriceList[i].toFloat()
                if (payer == username) {
                    amountPaid += price
                }
                totalPaid += price
            }

            // set textviews
            // amountPaidText.text = String.format("%.2f",-amountPaid) // round amount to two decimal places
            receiptTitleText.text = receipt.title
            receiptDateText.text = receipt.date

        } catch (e: NumberFormatException) {
            // set textviews
            amountPaidText.text = Globals.AMOUNT_PAID_ERROR
            receiptTitleText.text = receipt.title
            receiptDateText.text = receipt.date
        }

        // if user paid the bill
        if (!requestor.equals(username)) {
            val amountPaidString = String.format("%.2f", amountPaid)
            val totalPaidString = String.format("%.2f", totalPaid)
            amountPaidText.text = Html.fromHtml("You paid @$requestor <font color='#FF160C'>$$amountPaidString</font> for a $$totalPaidString bill")
            historyIcon.setBackgroundResource(R.drawable.ic_money_paid);
        } else {
            var payers = ""
            val translatedPayerList = Globals.Byte2ArrayList(receipt.payerList)
            val seenPayers = mutableSetOf<String>()
            for (i in 0 until translatedPayerList.size) {
                val payer = translatedPayerList[i]
                // ignore own username
                if (payer == username || seenPayers.contains(payer)) {
                    continue
                }
                seenPayers.add(payer)
                payers += "@$payer, "
            }
            if (payers.isEmpty()) {
                historyIcon.setBackgroundResource(R.drawable.ic_money_paid);
                val amountPaidString = String.format("%.2f", amountPaid)
                amountPaidText.text = Html.fromHtml("You paid a <font color='#FF160C'>$$amountPaidString</font> bill")
            } else {
                historyIcon.setBackgroundResource(R.drawable.ic_money_received);
                // get rid of ending space and comma
                payers = payers.substring(0, payers.length - 2)
                val amountReceivedString = String.format("%.2f", totalPaid - amountPaid)
                val totalPaidString = String.format("%.2f", totalPaid)
                amountPaidText.text = Html.fromHtml("$payers paid you <font color='#006400'>$$amountReceivedString</font> for a <font color='#FF160C'>$$totalPaidString</font> bill")
            }
        }
        receiptTitleText.text = receipt.title
        receiptDateText.text = receipt.date

        return view
    }

    fun replace(newHistoryList: List<ReceiptEntry>){
        historyList = newHistoryList
    }
}
