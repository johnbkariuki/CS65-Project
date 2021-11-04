package com.example.checkmate.console

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.checkmate.R

class LoginActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    fun onLoginButtonClick(view: View) {
        val etUserName = findViewById<EditText>(R.id.etUserName)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        val username = etUserName.text.toString()
        val password = etPassword.text.toString()

        if (username == "" || password == "") {
            Toast.makeText(this, "username or password blank", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(this, CodeActivity::class.java)
        val bundle = Bundle()
        bundle.putString("username", username)
        bundle.putString("password", password)
        intent.putExtras(bundle)
        startActivity(intent)
        finish()
    }

    fun onLoginBackClick(view: View) {
        startActivity(Intent(this, VenmoMainActivity::class.java))
        finish()
    }

}