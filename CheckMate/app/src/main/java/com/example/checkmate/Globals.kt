package com.example.checkmate

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
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
        val PAYER_STR = "Payer:"
        val PRICE_TYPE = "price type"  // for when processing text
        val QUANTITY_TYPE = "quantity type"
        val ITEM_TYPE = "item type "
        val RECEIPT_SUBMISSION_FAILURE = "there is nothing to save"

        // search bar
        val ADDED_PAYERS_KEY = "add payers key"
        val FRIEND_SEARCH_HINT = "Search your friends!"
        val EXISTING_PAYERS_KEY = "existing payers key"

        // receiptEntryListAdapter
        val HIDE_DROPDOWN = "hide dropdown"
        val SHOW_DROPDOWN = "show dropdown"
        val AMOUNT_PAID_ERROR = "Unable to Show Amount. Tap for more details"

        // profile, shared preferences
        val LOGGED_IN_KEY = "logged_in_key"
        val MY_PREFERENCES = "My_Preferences"

        // toasts
        const val RECEIPT_SUBMITTED_TOAST = "Receipt Submitted"

        // random
        val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "July", "Aug", "Sept", "Oct", "Nov", "Dec")

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