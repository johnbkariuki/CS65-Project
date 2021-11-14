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
class SelectedPayersListAdapter(val context: Context, var payersList: List<String>) : BaseAdapter() {

    var deletedUser: MutableLiveData<String> = MutableLiveData("")

    override fun getItem(position: Int): Any {
        return payersList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return payersList.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = View.inflate(context, R.layout.layout_selectedpayerslist_adapter, null)

        // get selected username
        val username = payersList[position]

        // get views, buttons
        val usernameTextView = view.findViewById<TextView>(R.id.payer_username)
        val deleteUserButton = view.findViewById<ImageButton>(R.id.payer_delete)

        usernameTextView.text = username
        // if user clicks delete
        deleteUserButton.setOnClickListener() {
            deletedUser.value = username
        }

        return view
    }
}

