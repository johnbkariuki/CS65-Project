package com.example.checkmate

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.math.floor

// for formatting items in the receipt list
class ReceiptListAdapter(val context: Context, var receiptList: List<Pair<String, Float>>) : BaseAdapter(){

    override fun getItem(position: Int): Any {
        return receiptList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return receiptList.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = View.inflate(context, R.layout.layout_receiptlist_adapter,null)
        val checkBox = view.findViewById<CheckBox>(R.id.receipt_checkbox)

        // display receipt item and price next to checkbox
        val receiptItem = receiptList[position]
        val item = receiptItem.first
        val price = receiptItem.second
        val lineDisplay = "$item ($$price)"
        checkBox.text = lineDisplay

        return view
    }
}