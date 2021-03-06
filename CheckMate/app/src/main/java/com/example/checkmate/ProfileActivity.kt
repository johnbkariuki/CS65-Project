package com.example.checkmate

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.net.wifi.ScanResult
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.theartofdev.edmodo.cropper.CropImage
import java.lang.StringBuilder
import com.google.firebase.database.DatabaseError

import androidx.annotation.NonNull
import com.example.checkmate.console.LoginActivity
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.signature.ObjectKey
import com.google.android.material.bottomnavigation.BottomNavigationView

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.getValue


class ProfileActivity : AppCompatActivity() {

    // views
    // private lateinit var logoutButton: Button
    private lateinit var saveButton: Button
    private lateinit var usernameText: TextView
    private lateinit var emailText: TextView
    private lateinit var phoneText: TextView
    private lateinit var imageView: ImageView
    private lateinit var takePhotoButton: Button
    private lateinit var cancelButton: Button

    // firebase and shared prefs
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mFirebaseFirestore: FirebaseFirestore
    private lateinit var mCurrUser: FirebaseUser
    private lateinit var mUserId: String
    private lateinit var pref: SharedPreferences
    private lateinit var storageReference: StorageReference

    // params
    private var email = ""
    private var password = ""
    private lateinit var imageUri: Uri
    private lateinit var profileUpdates: UserProfileChangeRequest
    private var SAVED_MESSAGE = "Changes Saved!"

    //map
    private lateinit var user: User

    // venmo
    private lateinit var venmoBtn: Button

    // used to crop image taken
    private val cropActivityResultContract = object: ActivityResultContract<Any, Uri?>(){
        override fun createIntent(context: Context, input: Any?): Intent {
            return CropImage.activity().getIntent(this@ProfileActivity)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return CropImage.getActivityResult(intent)?.uri
        }
    }

    private lateinit var cropActivityResultLauncher: ActivityResultLauncher<Any?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // get view
        imageView = findViewById(R.id.profileImg)

        // initialize cropping activity result
        cropActivityResultLauncher = registerForActivityResult(cropActivityResultContract){
            it?.let{ uri ->
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,uri)
                imageUri = uri
                imageView.setImageBitmap(bitmap)
                profileUpdates = UserProfileChangeRequest.Builder()
                    .setPhotoUri(imageUri)
                    .build()
            }
        }

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

            // get the firebase location the users image will be stored in
            storageReference = FirebaseStorage.getInstance().getReference("users/$mUserId")

            loadView()
        }

        // saveButton
        saveButton = findViewById(R.id.save_edit_button)
        saveButton.setOnClickListener {
            val editor: SharedPreferences.Editor = pref.edit()
            editor.putString(SignUpActivity.EMAIL_KEY, email)
            editor.putString(SignUpActivity.PASSWORD_KEY, password)
            editor.putBoolean(Globals.LOGGED_IN_KEY,true)
            editor.apply()

            // if there is a profile picture to save, save it
            if(this::profileUpdates.isInitialized){
                storageReference.putFile(imageUri)
            }

            Toast.makeText(this, SAVED_MESSAGE, Toast.LENGTH_SHORT).show()
        }

        cancelButton = findViewById(R.id.cancel_button_profile)
        cancelButton.setOnClickListener {
            finish()
        }

        // takePhotoButton
        takePhotoButton = findViewById(R.id.change_profile_photo_button)
        takePhotoButton.setOnClickListener {
            cropActivityResultLauncher.launch(null)
        }

        // venmo button
        venmoBtn = findViewById<Button>(R.id.venmo_profile_button)
        venmoBtn.setOnClickListener {
            updateVenmo()
        }

    }

    override fun onResume() {
        super.onResume()

        // grab existing email and pswd
        pref = getSharedPreferences(Globals.MY_PREFERENCES, Context.MODE_PRIVATE)
        email = pref.getString(SignUpActivity.EMAIL_KEY, "")!!
        password = pref.getString(SignUpActivity.PASSWORD_KEY, "")!!

        emailText.text = email

        // firestore specific
        mFirebaseFirestore.collection("users").document(mUserId).get()
            .addOnSuccessListener {
                usernameText.text = it.data!!["username"].toString()
                phoneText.text = it.data!!["phone"].toString()
            }
    }


    private fun loadView() {

        // get view components
        emailText = findViewById(R.id.email_profile)
        usernameText = findViewById(R.id.username_profile)
        phoneText = findViewById(R.id.phone_profile)

        emailText.text = email

        // firestore specific
        mFirebaseFirestore.collection("users").document(mUserId).get()
            .addOnSuccessListener {
                usernameText.text = it.data!!["username"].toString()
                phoneText.text = it.data!!["phone"].toString()
            }

        // get saved image from firestore and display it
        FirebaseStorage.getInstance().reference.child("users/$mUserId").downloadUrl.addOnSuccessListener {
            Glide.with(this).load(it).signature(ObjectKey(System.currentTimeMillis().toString())).into(imageView)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.edit_profile, menu)
        return true
    }

    // profile menu bar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            // start the update profile activity
            R.id.edit_profile_button -> {
                Toast.makeText(this, "You can now edit your profile", Toast.LENGTH_SHORT).show()
                val updateProfileIntent = Intent(this,UpdateProfileActivity::class.java)
                startActivity(updateProfileIntent)
                true
            }
            // log out of your account
            R.id.logout_profile_button -> {
                val editor: SharedPreferences.Editor = pref.edit()
                editor.putString(SignUpActivity.EMAIL_KEY, "")
                editor.putString(SignUpActivity.PASSWORD_KEY, "")
                editor.putBoolean(Globals.LOGGED_IN_KEY,false)
                editor.apply()

                // logout
                mFirebaseAuth.signOut()

                val intent = Intent(this, SignInSignUpActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateVenmo() {
        startActivity(Intent(this, LoginActivity::class.java))
    }
}