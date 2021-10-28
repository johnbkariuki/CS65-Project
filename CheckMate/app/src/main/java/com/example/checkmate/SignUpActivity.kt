package com.example.checkmate

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class SignUpActivity: AppCompatActivity() {
    private lateinit var mainActivityIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
    }

    fun onSignUpSubmitClicked(view: View){
        val pref: SharedPreferences = getSharedPreferences(MainActivity.MY_PREFERENCES, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = pref.edit()
        editor.putBoolean(MainActivity.LOGGED_IN_KEY,true)
        editor.commit()

        mainActivityIntent = Intent(this,MainActivity::class.java)
        startActivity(mainActivityIntent)
    }

    fun onSignUpCancelClicked(view: View){
        finish()
    }
}