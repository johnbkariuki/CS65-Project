package com.example.checkmate

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.FirebaseFirestore

class UserLookupActivity: AppCompatActivity() {
    // views
    private lateinit var search: SearchView
    private lateinit var userList: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var mFirestore: FirebaseFirestore
    private val users = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_lookup)

        // searchview and listview
        search = findViewById(R.id.searchView)
        userList = findViewById(R.id.listView)
        adapter = UserLookupAdapter(this, R.layout.user_lookup_adapter, users)
        userList.adapter = adapter

        // firestore and search
        mFirestore = FirebaseFirestore.getInstance()
        search.setOnQueryTextListener(object:SearchView.OnQueryTextListener {
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

        userList.setOnItemClickListener { parent, view, position, id ->
            val selected = parent.getItemAtPosition(position)
            val bundle = Bundle()
            bundle.putString("key", selected.toString())

            // start user retrieved intent
            val intent = Intent(this, UserRetrievedActivity::class.java)
            intent.putExtra("bundle", bundle)
            startActivity(intent)
        }
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.user_lookup_result, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.back -> {
                finish()
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}