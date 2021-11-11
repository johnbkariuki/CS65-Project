package com.example.checkmate.console

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.checkmate.R

class VenmoMainActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_venmo_main)
    }

    fun clickToLogin(view: View) {
        startActivity(Intent(this, LoginActivity::class.java))
    }

    fun clickToPayment(view: View) {
        startActivity(Intent(this, PaymentActivity::class.java))
    }

}