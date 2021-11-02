package com.example.checkmate

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class SignInSignUpActivity: AppCompatActivity() {
    private lateinit var signInIntent: Intent
    private lateinit var signUpintent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin_signup)
    }

    fun onSignInClicked(view: View){
        signInIntent = Intent(this,SignInActivity::class.java)
        startActivity(signInIntent)
    }

    fun onSignUpClicked(view: View){
        signUpintent = Intent(this,SignUpActivity::class.java)
        startActivity(signUpintent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }
}