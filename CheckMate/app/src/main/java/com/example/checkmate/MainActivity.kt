package com.example.checkmate

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private var loggedIn = false
    private lateinit var logInIntent: Intent
    private lateinit var pref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mFirebaseUser: FirebaseUser
    private lateinit var mUserId: String
    private lateinit var mDatabase: DatabaseReference
    private lateinit var mFirebaseFirestore: FirebaseFirestore
    private lateinit var mCurrUser: FirebaseUser

    private lateinit var username: String

    companion object {
        val LOGGED_IN_KEY = "logged_in_key"
        val MY_PREFERENCES = "My_Preferences"
        val USERNAME_KEY = "username key"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pref = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE)
        loggedIn = pref.getBoolean(LOGGED_IN_KEY, false)
//        loggedIn = false

        if(!loggedIn){
            logInIntent = Intent(this, SignInSignUpActivity::class.java)
            startActivity(logInIntent)
        } else {
            // setting up bottom navigation menu
            val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
            val navController = findNavController(R.id.nav_fragment)
            bottomNavigationView.setupWithNavController(
                navController
            )
            // get user permissions
            Util.checkPermissions(this)
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        /*
        editor = pref.edit()
        editor.putBoolean(LOGGED_IN_KEY,false)
        editor.commit()
        println("debug destroy:${pref.getBoolean(LOGGED_IN_KEY, false)}")
        */
    }

    /*
    override fun onBackPressed() {
        //super.onBackPressed()
        /*
        editor = pref.edit()
        if (loggedIn) editor.putBoolean(LOGGED_IN_KEY,true)
        else editor.putBoolean(LOGGED_IN_KEY, false)
        editor.commit()

         */

        //logInIntent = Intent(this, SignInSignUpActivity::class.java)
        //startActivity(logInIntent)
    }
     */
}