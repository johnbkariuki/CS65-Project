package com.example.checkmate

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class UserLookupAdapter(private val mContext: Context, private val mResource: Int, private val mArr: ArrayList<String>):
    ArrayAdapter<String>(mContext, mResource, mArr) {

    private lateinit var mFirestore: FirebaseFirestore

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        // firestore
        mFirestore = FirebaseFirestore.getInstance()

        // newview
        val newView = View.inflate(mContext, mResource, null)
        val username = mArr[position]

        val userImage: ImageView = newView!!.findViewById(R.id.image)
        val usernameText: TextView = newView!!.findViewById(R.id.username)
        usernameText.text = username

        // query database for userimage
        // if exists --> input
        // if doesn't exist --> use default_image.png
        mFirestore.collection("users").whereEqualTo("username", username).get()
            .addOnCompleteListener {
                var mUserId: String
                for (value in it.result.documents) {
                    mUserId = value.id

                    FirebaseStorage.getInstance().reference.child("users/$mUserId").downloadUrl.addOnSuccessListener {
                        Glide.with(mContext).load(it).into(userImage)
                    }
                        .addOnFailureListener { Glide.with(mContext).load(R.drawable.default_image).into(userImage) }
                }
            }
        return newView
    }
}