package com.example.checkmate

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.*

class SignInActivity : AppCompatActivity() {
    // firebase login
    private lateinit var mainActivityIntent: Intent
    private lateinit var mFirebaseAuth: FirebaseAuth

    // edittexts
    private lateinit var emailText: EditText
    private lateinit var passwordText: EditText

    // others
    private val TOAST_TEXT = "Successfully logged in!"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        // load edittext objects
        emailText = findViewById(R.id.email_sign_in_edittext)
        passwordText = findViewById(R.id.password_sign_in_edittext)

        // firebase
        mFirebaseAuth = FirebaseAuth.getInstance()
    }

    @Throws(FirebaseAuthInvalidUserException::class)
    fun onSignInSubmitClicked(view: View){

        // grab email and password
        val email = emailText.text.toString().trim()
        val password = passwordText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.signin_error_message3)
            builder.setTitle(R.string.signin_error_title)
            builder.setPositiveButton(android.R.string.ok, null)

            val dialog = builder.create()
            dialog.show()
        }
        else {
            // try-catch block for signing in
            try {
                mFirebaseAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener {

                    // shared prefs
                    val pref: SharedPreferences = getSharedPreferences(MainActivity.MY_PREFERENCES, Context.MODE_PRIVATE)
                    val editor: SharedPreferences.Editor = pref.edit()
                    editor.putString(SignUpActivity.EMAIL_KEY, email)
                    editor.putString(SignUpActivity.PASSWORD_KEY, password)
                    editor.putBoolean(MainActivity.LOGGED_IN_KEY,true)
                    editor.apply()

                    mainActivityIntent = Intent(this,MainActivity::class.java)
                    startActivity(mainActivityIntent)

                    // make a toast
                    val toast = Toast.makeText(this, TOAST_TEXT, Toast.LENGTH_LONG)
                    toast.show()
                }
                    .addOnFailureListener {
                        val builder = AlertDialog.Builder(this)
                        builder.setMessage(R.string.signin_error_message4)
                        builder.setTitle(R.string.signin_error_title)
                        builder.setPositiveButton(android.R.string.ok, null)

                        val dialog = builder.create()
                        dialog.show()
                    }
            }
            // invalid username
            catch (invalid: FirebaseAuthInvalidUserException) {
                val builder = AlertDialog.Builder(this)
                builder.setMessage(R.string.signin_error_message)
                builder.setTitle(R.string.signin_error_title)
                builder.setPositiveButton(android.R.string.ok, null)

                val dialog = builder.create()
                dialog.show()
            }
            // invalid password
            catch (invalid: FirebaseAuthInvalidCredentialsException) {
                val builder = AlertDialog.Builder(this)
                builder.setMessage(R.string.signin_error_message2)
                builder.setTitle(R.string.signin_error_title)
                builder.setPositiveButton(android.R.string.ok, null)

                val dialog = builder.create()
                dialog.show()
            }
        }
    }

    fun onSignInCancelClicked(view: View){
        finish()
    }
}