package com.example.checkmate

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.math.log

class MainActivity : AppCompatActivity() {
    private var loggedIn = false
    private lateinit var logInIntent: Intent
    private lateinit var pref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pref = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE)
        loggedIn = pref.getBoolean(LOGGED_IN_KEY, false)

        println("debug: $loggedIn")

        if(!loggedIn){
            logInIntent = Intent(this, SignInSignUpActivity::class.java)
            startActivity(logInIntent)
        } else{
            // setting up bottom navigation menu
            val bottomNavigationView = findViewById<BottomNavigationView
                    >(R.id.bottom_navigation_view)
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
        println("Debug: Here")
    }

    override fun onDestroy() {
        super.onDestroy()
        editor = pref.edit()
        editor.putBoolean(LOGGED_IN_KEY,false)
        editor.commit()
        println("debug destroy:${pref.getBoolean(LOGGED_IN_KEY, false)}")
    }

    override fun onBackPressed() {
        super.onBackPressed()
        editor = pref.edit()
        editor.putBoolean(LOGGED_IN_KEY,false)
        editor.commit()

        logInIntent = Intent(this, SignInSignUpActivity::class.java)
        startActivity(logInIntent)
    }

    companion object{
        val LOGGED_IN_KEY = "logged_in_key"
        val MY_PREFERENCES = "My_Preferences"
    }
}