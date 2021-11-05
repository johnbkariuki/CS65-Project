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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity: AppCompatActivity() {
    companion object {
        const val EMAIL_KEY = "username"
        const val PASSWORD_KEY = "password"
    }

    private lateinit var mainActivityIntent: Intent
    private lateinit var usernameText: EditText
    private lateinit var emailText: EditText
    private lateinit var passwordText: EditText
    private lateinit var venmoText: EditText
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mFirebaseUser: FirebaseUser
    private lateinit var mUserId: String
    private lateinit var mDatabase: DatabaseReference
    private lateinit var mFirebaseFirestore: FirebaseFirestore

    private val TOAST_TEXT = "Success! Profile Created!"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        usernameText = findViewById(R.id.username_edittext)
        emailText = findViewById(R.id.email_sign_up_edittext)
        passwordText = findViewById(R.id.password_sign_up_edittext)
        venmoText = findViewById(R.id.venmo_edittext)

        mFirebaseAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance().reference
        mFirebaseFirestore = FirebaseFirestore.getInstance()
    }

    fun onSignUpSubmitClicked(view: View){
        val pref: SharedPreferences = getSharedPreferences(Globals.MY_PREFERENCES, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = pref.edit()

        if (validateCredentials()) {

            // save email and password to log user back in later
            editor.putString(EMAIL_KEY, emailText.text.toString().trim())
            editor.putString(PASSWORD_KEY, passwordText.text.toString().trim())
            editor.putBoolean(Globals.LOGGED_IN_KEY, true)

            // apply changed and save to shared preferences
            editor.apply()

            // launch main activity
            mainActivityIntent = Intent(this, MainActivity::class.java)
            startActivity(mainActivityIntent)

            // make a toast
            val toast = Toast.makeText(this, TOAST_TEXT, Toast.LENGTH_LONG)
            toast.show()
        }
    }

    // helper function to check if all inputs have been correctly defined
    private fun validateCredentials(): Boolean {
        // check username
        val username = usernameText.text.toString().trim()
        // check email
        val email = emailText.text.toString().trim()
        // check password
        val password = passwordText.text.toString().trim()
        // check venmo
        val venmo = venmoText.text.toString().trim()

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() ||
                venmo.isEmpty()) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.signup_error_message)
            builder.setTitle(R.string.signup_error_title)
            builder.setPositiveButton(android.R.string.ok, null)

            val dialog = builder.create()
            dialog.show()
            return false
        }
        else if (password.length < 6) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.signup_error_message2)
            builder.setTitle(R.string.signup_error_title)
            builder.setPositiveButton(android.R.string.ok, null)

            val dialog = builder.create()
            dialog.show()
            return false
        }
        else {
            // create the user and add to the database
            mFirebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isComplete) {
                    mFirebaseAuth.signInWithEmailAndPassword(email, password)
                    if (mFirebaseAuth.currentUser != null) {
                        mFirebaseUser = mFirebaseAuth.currentUser!!
                        mUserId = mFirebaseUser.uid

                        // input into database
                        val user = User(username, email, password, venmo, generateKeywords(username))
                        mDatabase.child("users").child(mUserId).child("user").child("username")
                            .push().setValue(user.username)
                        mDatabase.child("users").child(mUserId).child("user").child("email")
                            .push()
                            .setValue(user.email)
                        mDatabase.child("users").child(mUserId).child("user").child("password")
                            .push().setValue(user.password)
                        mDatabase.child("users").child(mUserId).child("user").child("venmo")
                            .push()
                            .setValue(user.venmo)
                        mDatabase.child("users").child(mUserId).child("user").child("keywords")
                            .push()
                            .setValue(user.keywords)

                        // input into firestore
                        mFirebaseFirestore.collection("users").document(mUserId).set(user).addOnCompleteListener {
                            println("debug: @$mUserId successfully added to Firestore ($mFirebaseFirestore)")
                        }.addOnFailureListener {
                            println("debug: @$mUserId failed to be added to Firestore ($mFirebaseFirestore)")
                        }
                    }
                }
                else println("sign up failed")
            }
            return true
        }
    }

    private fun generateKeywords(username: String): List<String> {
        val keywords = mutableListOf<String>()
        for (i in 0 until username.length) {
            for (j in (i+1)..username.length) {
                keywords.add(username.slice(i until j))
            }
        }
        return keywords
    }

    fun onSignUpCancelClicked(view: View){
        finish()
    }
}