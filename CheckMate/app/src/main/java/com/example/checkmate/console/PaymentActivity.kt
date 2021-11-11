package com.example.checkmate.console

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.checkmate.R

class PaymentActivity: AppCompatActivity() {

    // Request lists
    private lateinit var amountsList: ArrayList<Double>
    private lateinit var notesList: ArrayList<String>
    private lateinit var idsList: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        amountsList = savedInstanceState?.getSerializable("amountsList") as ArrayList<Double>
        notesList = savedInstanceState?.getSerializable("notesList") as ArrayList<String>
        idsList = savedInstanceState?.getSerializable("idsList") as ArrayList<String>
    }

    fun onPaymentButtonClick(view: View) {
        val etUserName = findViewById<EditText>(R.id.etUserName)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        val username = etUserName.text.toString()
        val password = etPassword.text.toString()

        if (username == "" || password == "") {
            Toast.makeText(this, "username or password blank", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(this, RequestActivity::class.java)
        val bundle = Bundle()
        bundle.putString("username", username)
        bundle.putString("password", password)

        // Request lists
        bundle.putSerializable("amountsList", amountsList)
        bundle.putSerializable("notesList", notesList)
        bundle.putSerializable("idsList", idsList)

        intent.putExtras(bundle)
        startActivity(intent)
        finish()
    }

    fun onPaymentBackClick(view: View) {
        startActivity(Intent(this, VenmoMainActivity::class.java))
        finish()
    }

}