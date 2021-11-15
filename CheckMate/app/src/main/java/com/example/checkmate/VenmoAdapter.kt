package com.example.checkmate

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.Python
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class VenmoAdapter: AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mFirebaseFirestore: FirebaseFirestore
    private lateinit var mCurrUser: FirebaseUser
    private lateinit var mUserId: String
    private lateinit var pref: SharedPreferences
    private var email = ""
    private var password = ""

    override fun onCreate(bundle: Bundle?) {
        println("debug: venmoadapter oncreate")
        super.onCreate(bundle)

        val strBundle = intent.extras
        val venmoId: String? = strBundle?.getString("venmoId")
        val unsuccessful: String? = strBundle?.getString("unsuccessful")

        if(venmoId != null) {
            println("debug: venmoid not null")
            addId(venmoId)
        } else if(unsuccessful != null) {
            handleUnsuccessful(unsuccessful)
        }
        if(venmoUserSignUp) {
            startActivity(Intent(this, MainActivity::class.java))
            venmoUserSignUp = false
        } else {
            // discuss with jason
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        finish()
    }

    private fun addId(str: String) {
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
            // loadView()
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("users")
        databaseReference.child(mUserId).child("user").child("venmo")
            .setValue(str)      // String is venmo id

        mFirebaseFirestore.collection("users").document(mUserId).update(
            mapOf(
                "venmo" to str     // String is venmo id
            )
        )
        println("debug: venmo ID updated $str")
        Toast.makeText(this, "Venmo ID updated $str", Toast.LENGTH_SHORT).show()
    }

    private fun handleUnsuccessful(str: String) {
        println("debug: unsuccessful: $str")
        Toast.makeText(this, "Unsuccessful ids: $str", Toast.LENGTH_LONG).show()
    }

}