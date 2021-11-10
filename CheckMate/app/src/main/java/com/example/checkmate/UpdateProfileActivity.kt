package com.example.checkmate

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class UpdateProfileActivity:AppCompatActivity() {
    private lateinit var usernameText: EditText
    private lateinit var emailText: EditText
    private lateinit var venmoText: EditText
    private lateinit var passwordText: EditText
    private lateinit var saveBtn: Button
    private lateinit var cancelBtn: Button
    private var email = ""
    private var password = ""
    private var SAVED_UPDATE_MESSAGE = "Profile Updated!"


    private lateinit var databaseReference: DatabaseReference
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mFirebaseFirestore: FirebaseFirestore
    private lateinit var mCurrUser: FirebaseUser
    private lateinit var mUserId: String
    private lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_profile)

        usernameText = findViewById<EditText>(R.id.update_username_profile)
        emailText = findViewById<EditText>(R.id.update_email_profile)
        venmoText = findViewById<EditText>(R.id.update_venmo_profile)
        passwordText = findViewById<EditText>(R.id.update_password_profile)
        saveBtn = findViewById<Button>(R.id.update_profile_save_button)
        cancelBtn = findViewById<Button>(R.id.update_profile_cancel_button)


        // grab existing email and pswd
        pref = getSharedPreferences(Globals.MY_PREFERENCES, Context.MODE_PRIVATE)
        email = pref.getString(SignUpActivity.EMAIL_KEY, "")!!
        password = pref.getString(SignUpActivity.PASSWORD_KEY, "")!!

        // firebase and load the view
        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseFirestore = FirebaseFirestore.getInstance()
        mFirebaseAuth.signInWithEmailAndPassword(email, password)


        if (mFirebaseAuth.currentUser != null) {
            mCurrUser = mFirebaseAuth.currentUser!!
            mUserId = mCurrUser.uid
            loadView()
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("users")

        // saveButton
        saveBtn.setOnClickListener {
            update_info()
        }
        cancelBtn.setOnClickListener {
            finish()
        }
    }


    private fun update_info(){

        if(emailText.text.isNotEmpty() && usernameText.text.isNotEmpty() && venmoText.text.isNotEmpty() && passwordText.text.isNotEmpty()) {

            databaseReference.child(mUserId).child("user").child("email")
                .setValue(emailText.text.toString())
            databaseReference.child(mUserId).child("user").child("username")
                .setValue(usernameText.text.toString())
            databaseReference.child(mUserId).child("user").child("venmo")
                .setValue(venmoText.text.toString())
            databaseReference.child(mUserId).child("user").child("password")
                .setValue(passwordText.text.toString())

            mFirebaseFirestore.collection("users").document(mUserId).update(
                mapOf(
                    "email" to emailText.text.toString(),
                    "password" to passwordText.text.toString(),
                    "username" to usernameText.text.toString(),
                    "venmo" to venmoText.text.toString(),
                    "keywords" to generateKeywords(usernameText.text.toString())
                )
            )

            mCurrUser.updateEmail(emailText.text.toString())
            mCurrUser.updatePassword(passwordText.text.toString())

            //update email just incase it was changed
            email = emailText.text.toString()

            val editor: SharedPreferences.Editor = pref.edit()
            editor.putString(SignUpActivity.EMAIL_KEY, email)
            editor.putString(SignUpActivity.PASSWORD_KEY, password)
            editor.putBoolean(Globals.LOGGED_IN_KEY,true)
            editor.apply()

            Toast.makeText(this, SAVED_UPDATE_MESSAGE, Toast.LENGTH_LONG).show()

        } else {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.signup_error_message)
            builder.setTitle(R.string.signup_error_title)
            builder.setPositiveButton(android.R.string.ok, null)

            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun loadView() {
        emailText.setText(email)
        passwordText.setText(password)

        // firestore specific
        mFirebaseFirestore.collection("users").document(mUserId).get()
            .addOnSuccessListener {

                // println("debug: $it") // debugging purposes
                usernameText.setText(it.data!!["username"].toString())
                venmoText.setText(it.data!!["venmo"].toString())
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
}