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

    private lateinit var selectPayerButton: Button
//    private var payers = mutableMapOf<Int, String>()  // key = row, value = username

    private val _payersMap = MutableLiveData<MutableMap<Int, String>>()
    val payersMap: LiveData<MutableMap<Int, String>>
        get() {
            return _payersMap
        }

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
        val receiptLine = view.findViewById<TextView>(R.id.receipt_line)

        // display receipt item and price next to checkbox
        val receiptItem = receiptList[position]
        val item = receiptItem.first
        val price = receiptItem.second
        val lineDisplay = "$item ($$price)"
        receiptLine.text = lineDisplay

        selectPayerButton = view.findViewById<Button>(R.id.select_payer_button)
//        var payerString = "${ReceiptActivity.PAYER_STR} $currPayer"
        var payerString = "${ReceiptActivity.PAYER_STR} "

        selectPayerButton.text = payerString
        selectPayerButton.setOnClickListener {
            showPopupMenu(selectPayerButton, position)
        }
        return view
    }

    // allows user to select current payer
    fun showPopupMenu(view: View, position: Int) {
        PopupMenu(view.context, view).apply {
            menuInflater.inflate(R.menu.popup_menu, menu)

            setOnMenuItemClickListener { item ->
                Toast.makeText(view.context, "You Clicked : " + item.title, Toast.LENGTH_SHORT).show()
                _payersMap.value?.set(position, item.title.toString())
                true
            }
        }.show()
    }
}