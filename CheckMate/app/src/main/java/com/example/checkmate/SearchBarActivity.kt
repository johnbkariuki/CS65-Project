package com.example.checkmate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchBarActivity : AppCompatActivity() {

    // listview
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var adapterSelected: ArrayAdapter<String>
    private lateinit var searchedList: ListView
    private lateinit var selectedList: ListView

    // Firestore/Firebase
    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var mFirebaseAuth: FirebaseAuth

    // selected users
    private var selectedUsers = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_bar)

        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirestore = FirebaseFirestore.getInstance()

        searchedList = findViewById(R.id.searched_list)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        searchedList.adapter = adapter

        searchedList.setOnItemClickListener { parent, view, position, id ->
            val selected = parent.getItemAtPosition(position)
            if (!selectedUsers.contains(selected.toString())) {
                selectedUsers.add(selected.toString())

                // add to selected
                adapterSelected.clear()
                adapterSelected.addAll(selectedUsers)
                adapterSelected.notifyDataSetChanged()
            }
            else {
                // make a toast
                val toast = Toast.makeText(this, "This user has already been selected!", Toast.LENGTH_SHORT).show()
                toast!! // don't need the non-null assertion, just thought it was funny
            }
        }

        selectedList = findViewById(R.id.selected_list)
        adapterSelected = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        selectedList.adapter = adapterSelected

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_bar, menu)

        val search = menu?.findItem(R.id.user_search)
        val searchView = search?.actionView as SearchView
        searchView.queryHint = "Search your friends!"
        searchView.setOnQueryTextListener(object:SearchView.OnQueryTextListener {
            // add the user to the listview
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            // query the database for users
            override fun onQueryTextChange(newText: String?): Boolean {
                getUsersByUsername(newText!!)
                return true
            }
        })

        return super.onCreateOptionsMenu(menu)
    }

    private fun getUsersByUsername(query: String) {
        val users = ArrayList<String>()
        mFirestore.collection("users")
            .whereArrayContains("keywords", query.trim())
            .limit(50)
            .get()
            .addOnCompleteListener(OnCompleteListener {
                for (user in it.result.documents) {
                    val username = user.data!!["username"].toString()
                    users.add(username)
                }
                adapter.clear()
                adapter.addAll(users)
                adapter.notifyDataSetChanged()
            })
    }

    fun onSelectFriendsCLicked(view: View) {
//        val intent = Intent(this, ReceiptActivity::class.java)
//        val bundle = Bundle()
//        bundle.putStringArrayList("users", selectedUsers)
//        intent.putExtras(bundle)
//        startActivity(intent)

        // ** Brandon revision
        val pref = getSharedPreferences(MainActivity.MY_PREFERENCES, Context.MODE_PRIVATE)
        val editor = pref.edit()

        val selectedUsersSet: Set<String> = selectedUsers.toSet()
        editor.putStringSet(Globals.SELECTED_USERS_KEY, selectedUsersSet)
        editor.apply()

        finish()
    }
}