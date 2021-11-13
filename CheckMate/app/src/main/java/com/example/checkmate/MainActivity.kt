package com.example.checkmate

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.SearchView
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private var loggedIn = false
    private lateinit var logInIntent: Intent
    private lateinit var pref: SharedPreferences


    // Firestore/Firebase
    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var mFirebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pref = getSharedPreferences(Globals.MY_PREFERENCES, Context.MODE_PRIVATE)
        loggedIn = pref.getBoolean(Globals.LOGGED_IN_KEY, false)

        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirestore = FirebaseFirestore.getInstance()

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.lookup_search, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.user_search -> {
                val intent = Intent(this, UserLookupActivity::class.java)
                startActivity(intent)
                true
            }
            else-> return super.onOptionsItemSelected(item)
        }
    }
}