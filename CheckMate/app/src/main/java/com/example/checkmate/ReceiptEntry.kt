
package com.example.checkmate

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "receipt_entry_table")
data class ReceiptEntry (

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,

    @ColumnInfo(name = "title_column")
    var title: String = "No Title",  // title given by user to receipt

    @ColumnInfo(name = "date_column")
    var date: String = "",  // date receipt was taken

    @ColumnInfo(name = "payer_column")
    var payer: String = "",  // username of payer who paid whole bill

    @ColumnInfo(name = "price_list_column")
    var priceList: ByteArray = byteArrayOf(),  // list of prices (strings)

    @ColumnInfo(name = "item_list_column")
    var itemList: ByteArray = byteArrayOf(),  // list of items (strings)

    @ColumnInfo(name = "payer_list_item")
    var payerList: ByteArray = byteArrayOf()  // list of payer usernames (strings)
)