package com.example.checkmate.console

import android.content.Intent
import android.opengl.Visibility
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.checkmate.*

class LoginActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if(venmoUserSignUp) {
            val backButton = findViewById<Button>(R.id.venmoBtnBack)
            backButton.visibility = View.INVISIBLE
        }
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
        bundle.putBoolean("fromLogin", true)
        intent.putExtras(bundle)

        // already running make sure not running anymore
        if(!Python.isStarted()) {
            Python.start(AndroidPlatform(applicationContext))
        }

        startActivity(intent)
        finish()
    }

    fun onLoginBackClick(view: View) {
        finish()
    }

}