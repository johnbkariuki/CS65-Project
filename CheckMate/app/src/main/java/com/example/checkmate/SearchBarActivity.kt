package com.example.checkmate

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.provider.BaseColumns
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.Observer

class  SearchBarActivity : AppCompatActivity() {

    // listview
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var adapterSelected: SelectedPayersListAdapter
    private lateinit var searchedList: ListView
    private lateinit var selectedList: ListView
    private lateinit var cursorAdapter: SimpleCursorAdapter

    // Firestore/Firebase
    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mUserId: String
    private lateinit var mCurrUser: FirebaseUser

    // selected users
    private var selectedUsers = ArrayList<String>()
    private var suggestions = ArrayList<String>()

    private var payer = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_bar)

        val pref = getSharedPreferences(Globals.MY_PREFERENCES, Context.MODE_PRIVATE)
        val email = pref.getString(SignUpActivity.EMAIL_KEY, "")!!
        val password = pref.getString(SignUpActivity.PASSWORD_KEY, "")!!

        // firebase and load the view
        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirestore = FirebaseFirestore.getInstance()
        mFirebaseAuth.signInWithEmailAndPassword(email, password)

        if (mFirebaseAuth.currentUser != null) {
            mCurrUser = mFirebaseAuth.currentUser!!
            mUserId = mCurrUser.uid
        }

        mFirestore.collection("users").document(mUserId).get()
            .addOnSuccessListener { entry ->
                // get username - add to popup buttons
                payer = entry.data!!["username"].toString()
                mFirestore.collection("users")
                    .get()
                    .addOnCompleteListener(OnCompleteListener { it ->
                        for (user in it.result.documents) {
                            val username = user.data!!["username"].toString()
                            if (username != payer) {
                                suggestions.add(username)
                            }
                        }
                    })
            }



        selectedList = findViewById(R.id.selected_list)
        adapterSelected = SelectedPayersListAdapter(this, selectedUsers)
        selectedList.adapter = adapterSelected
        adapterSelected.deletedUser.observe(this, Observer { it ->
            if (selectedUsers.contains(it)) {
                selectedUsers.remove(it)
                adapterSelected.notifyDataSetChanged()
            }
        })

        // setting up search bar
        val searchView = findViewById<SearchView>(R.id.user_search)
        searchView.queryHint = Globals.FRIEND_SEARCH_HINT

        val from = arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1)
        val to = intArrayOf(R.id.search_suggestion)
        cursorAdapter = SimpleCursorAdapter(this, R.layout.layout_search_suggestion, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER)
        searchView.suggestionsAdapter = cursorAdapter

        searchView.setOnQueryTextListener(object:SearchView.OnQueryTextListener {
            // add the user to the listview
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!selectedUsers.contains(query)) {
                    if (suggestions.contains(query)) {
                        selectedUsers.add(query!!)

                        // add to selected
                        adapterSelected.notifyDataSetChanged()
                    } else {
                        val toastMessage = "User not found!"
                        val toast = Toast.makeText(this@SearchBarActivity, toastMessage, Toast.LENGTH_SHORT).show()
                    }
                }
                else {
                    // make a toast
                    val toastMessage = "This user has already been selected!"
                    val toast = Toast.makeText(this@SearchBarActivity, toastMessage, Toast.LENGTH_SHORT).show()
                }
                return false
            }
//
            // query the database for users
            override fun onQueryTextChange(newText: String?): Boolean {
                getUsersByUsername(newText!!)
                return true
            }
        })

        searchView.setOnSuggestionListener(object: SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            override fun onSuggestionClick(position: Int): Boolean {
//                hideKeyboard()
                val cursor = searchView.suggestionsAdapter.getItem(position) as Cursor
                val selected = cursor.getString(cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_1))
                searchView.setQuery(selected, false)

//                val selected = parent.getItemAtPosition(position)


                // Do something with selection
                return true
            }
        })

    }

    private fun getUsersByUsername(query: String) {
        val cursor = MatrixCursor(arrayOf(BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1))
        query?.let {
            suggestions.forEachIndexed { index, suggestion ->
                if (suggestion.contains(query, true)) {
                    println("debug: added ($index, $suggestion) to cursor")
                    cursor.addRow(arrayOf(index, suggestion))
                }
            }
        }
        cursorAdapter.changeCursor(cursor)
    }

    fun onSelectFriendsCLicked(view: View) {

        val pref = getSharedPreferences(Globals.MY_PREFERENCES, Context.MODE_PRIVATE)
        val editor = pref.edit()

        val selectedUsersSet: Set<String> = selectedUsers.toSet()
        editor.putStringSet(Globals.ADDED_PAYERS_KEY, selectedUsersSet)
        editor.apply()

        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val pref = getSharedPreferences(Globals.MY_PREFERENCES, Context.MODE_PRIVATE)
        val editor = pref.edit()

        val selectedUsersSet: Set<String> = selectedUsers.toSet()
        editor.putStringSet(Globals.ADDED_PAYERS_KEY, selectedUsersSet)
        editor.apply()

        finish()
        return true
    }
}