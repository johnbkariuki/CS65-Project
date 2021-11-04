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

    // key = row position, value = payer username
    private val payersMapStore = mutableMapOf<Int, String>()
    private val _payersMap = MutableLiveData<MutableMap<Int, String>>()
    val payersMap: LiveData<MutableMap<Int, String>>
        get() {
            return _payersMap
        }

    companion object {
        const val PAYER_STR = "Payer:"
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
        println("debug: getView called")

        val view: View = View.inflate(context, R.layout.layout_receiptlist_adapter,null)
        val receiptLine = view.findViewById<TextView>(R.id.receipt_line)

        // display receipt item and price next to checkbox
        val receiptItem = receiptList[position]
        val item = receiptItem.first
        val price = receiptItem.second
        val lineDisplay = "$item ($$price)"
        receiptLine.text = lineDisplay

        // select payer popup menu
        selectPayerButton = view.findViewById<Button>(R.id.select_payer_button)
        var payer = ""
        // if payer has been selected, display
        if (payersMapStore.containsKey(position)) {
            payer = payersMapStore[position].toString()
        }
        val payerString = "$PAYER_STR $payer"
        selectPayerButton.text = payerString

        // listener for popup
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