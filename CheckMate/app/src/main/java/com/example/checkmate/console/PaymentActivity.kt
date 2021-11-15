package com.example.checkmate.console

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.checkmate.R
import com.example.checkmate.ReceiptActivity

class PaymentActivity: AppCompatActivity() {

    // Request lists
    private lateinit var amountsList: ArrayList<Double>
    private lateinit var notesList: ArrayList<String>
    private lateinit var idsList: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        val serBundle = intent.extras

        amountsList = serBundle?.getSerializable("amountsList") as ArrayList<Double>
        notesList = serBundle?.getSerializable("notesList") as ArrayList<String>
        idsList = serBundle?.getSerializable("idsList") as ArrayList<String>

        println("debug: amountsList $amountsList")
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
        bundle.putBoolean("fromLogin", false)

        // Request lists
        bundle.putSerializable("amountsList", amountsList)
        bundle.putSerializable("notesList", notesList)
        bundle.putSerializable("idsList", idsList)

        intent.putExtras(bundle)

        if(!Python.isStarted()) {
            Python.start(AndroidPlatform(applicationContext))
        }
        startActivity(intent)
        finish()
    }

    fun onPaymentBackClick(view: View) {
        finish()
    }

}