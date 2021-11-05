package com.example.checkmate

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class Globals {

    companion object {
        // *** VARIABLES *** //

        // receipt activity
        val RECEIPT_MODE_KEY = "receipt display mode key"
        val RECEIPT_HISTORY_MODE = "receipt history mode"
        val RECEIPT_NEW_MODE = "receipt new mode"
        val RECEIPT_TITLE_KEY = "receipt title key"
        val RECEIPT_PRICELIST_KEY = "receipt pricelist key"
        val RECEIPT_ITEMLIST_KEY = "receipt itemlist key"
        val RECEIPT_PAYERLIST_KEY = "receipt payerlist key"

        // search bar
        val SELECTED_USERS_KEY = "selected users key"

        // receiptEntryListAdapter
        val HIDE_POPUP = "hide popup"
        val SHOW_POPUP = "show popup"

        // *** METHODS *** //

        // convert arraylist to byte array for database storage
        fun ArrayList2Byte(list: ArrayList<String>): ByteArray {

            val bos = ByteArrayOutputStream()
            val oos = ObjectOutputStream(bos)
            oos.writeObject(list)

            return bos.toByteArray()
        }

        // convert byte array from database storage back into array list of strings
        fun Byte2ArrayList(byteArray: ByteArray): ArrayList<String> {

            val bis = ByteArrayInputStream(byteArray)
            val ois = ObjectInputStream(bis)

            return ois.readObject() as ArrayList<String>
        }
    }


}