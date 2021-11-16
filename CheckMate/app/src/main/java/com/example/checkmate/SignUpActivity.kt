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
import com.example.checkmate.console.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

var venmoUserSignUp = false

class SignUpActivity: AppCompatActivity() {
    companion object {
        const val EMAIL_KEY = "username"
        const val PASSWORD_KEY = "password"
    }

    private lateinit var venmoActivityIntent: Intent
    private lateinit var usernameText: EditText
    private lateinit var emailText: EditText
    private lateinit var passwordText: EditText
    private lateinit var phoneText: EditText
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mFirebaseUser: FirebaseUser
    private lateinit var mUserId: String
    private lateinit var mDatabase: DatabaseReference
    private lateinit var mFirebaseFirestore: FirebaseFirestore

    private var valid_sign_up = true

    private val TOAST_TEXT = "Success! Profile Created!"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        usernameText = findViewById(R.id.username_edittext)
        emailText = findViewById(R.id.email_sign_up_edittext)
        passwordText = findViewById(R.id.password_sign_up_edittext)
        phoneText = findViewById(R.id.phone_edittext)


        mFirebaseAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance().reference
        mFirebaseFirestore = FirebaseFirestore.getInstance()
    }

    fun onSignUpSubmitClicked(view: View){
        validateCredentials()
    }

    // helper function to check if all inputs have been correctly defined
    private fun validateCredentials(){
        // check username
        val username = usernameText.text.toString().trim()
        // check email
        val email = emailText.text.toString().trim()
        // check password
        val password = passwordText.text.toString().trim()
        //check phone
        val phone = phoneText.text.toString().trim()
        // phone validation
        val phoneRegex = "^[+]?[0-9]{10,13}$".toRegex()

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.signup_error_message)
            builder.setTitle(R.string.signup_error_title)
            builder.setPositiveButton(android.R.string.ok, null)

            val dialog = builder.create()
            dialog.show()
        }
        else if (password.length < 6) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.signup_error_message2)
            builder.setTitle(R.string.signup_error_title)
            builder.setPositiveButton(android.R.string.ok, null)

            val dialog = builder.create()
            dialog.show()
        }
        else if (!phone.matches(phoneRegex)){
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.signup_phone_error_message)
            builder.setTitle(R.string.signup_error_title)
            builder.setPositiveButton(android.R.string.ok, null)

            val dialog = builder.create()
            dialog.show()
        }
        else {
            // create the user and add to the database
            mFirebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    mFirebaseAuth.signInWithEmailAndPassword(email, password)

                    if (mFirebaseAuth.currentUser != null) {
                        mFirebaseUser = mFirebaseAuth.currentUser!!
                        mUserId = mFirebaseUser.uid

                        // input into database
                        val user =
                            User(username, email, password, null, generateKeywords(username),phone, ArrayList<Receipt>())
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
                        mDatabase.child("users").child(mUserId).child("user").child("phone")
                            .push()
                            .setValue(user.phone)
                        mDatabase.child("users").child(mUserId).child("user").child("receipts")
                            .push()
                            .setValue(user.receipts)

                        // input into firestore
                        mFirebaseFirestore.collection("users").document(mUserId).set(user)
                            .addOnCompleteListener {
                                println("debug: @$mUserId successfully added to Firestore ($mFirebaseFirestore)")
                            }.addOnFailureListener {
                            println("debug: @$mUserId failed to be added to Firestore ($mFirebaseFirestore)")
                        }

                        val pref: SharedPreferences = getSharedPreferences(Globals.MY_PREFERENCES, Context.MODE_PRIVATE)
                        val editor: SharedPreferences.Editor = pref.edit()

                        // save email and password to log user back in later
                        editor.putString(EMAIL_KEY, emailText.text.toString().trim())
                        editor.putString(PASSWORD_KEY, passwordText.text.toString().trim())
                        editor.putBoolean(Globals.LOGGED_IN_KEY, true)

                        // apply changed and save to shared preferences
                        editor.apply()

                        // launch venmo registration activity
                        venmoUserSignUp = true
                        venmoActivityIntent = Intent(this, LoginActivity::class.java)
                        startActivity(venmoActivityIntent)

                        // make a toast
                        val toast = Toast.makeText(this, TOAST_TEXT, Toast.LENGTH_LONG)
                        toast.show()

                        // finish this activity
                        finish()
                    }

                } else {
                    val builder = AlertDialog.Builder(this)
                    builder.setMessage(R.string.used_email_error_message)
                    builder.setTitle(R.string.signup_error_title)
                    builder.setPositiveButton(android.R.string.ok, null)

                    val dialog = builder.create()
                    dialog.show()
                }
            }
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