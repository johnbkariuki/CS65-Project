package com.example.checkmate

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.widget.ArrayAdapter
import android.widget.Toast

import android.widget.AdapterView







// for formatting items in the receipt list
class ReceiptEntryListAdapter(val context: Context, var receiptList: List<Pair<String, String>>) : BaseAdapter() {

    var displayMode = Globals.SHOW_DROPDOWN
    var payers = arrayListOf<String>()

    // key = row position, value = payer username
    val payersMapStore = mutableMapOf<Int, String>()
    val _payersMap = MutableLiveData<MutableMap<Int, String>>()
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

        // select payer dropdown menu
        val selectPayerButton = view.findViewById<Button>(R.id.select_payer_button)

//        var payer = ""
        // if payer has been selected, display
        if (payersMapStore.containsKey(position)) {
            val payer = payersMapStore[position].toString()
            selectPayerButton.text = payer
        }
//        val payerString = "$payer"

        // if creatingn new receipt, show popup
        if (displayMode == Globals.SHOW_DROPDOWN) {
            // listener for popup
            selectPayerButton.setOnClickListener {
                showPopupMenu(selectPayerButton, position)
            }
        }

        return view
    }

    // allows user to select current payer
    fun showPopupMenu(view: View, position: Int) {
        PopupMenu(view.context, view).apply {
            for (payer in payers) {
                menu.add(payer)
            }

            menuInflater.inflate(R.menu.popup_menu, menu)
            setOnMenuItemClickListener { item ->
                // update map with new payer
                payersMapStore[position] = item.title.toString()
                _payersMap.value = payersMapStore
                val payersMapPrint = _payersMap.value
                println("debug: payersMap = $payersMapPrint")
                true
            }
        }.show()
    }
}
