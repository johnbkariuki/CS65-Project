package com.example.checkmate

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.theartofdev.edmodo.cropper.CropImage

class ProfileActivity : AppCompatActivity() {
    // views
    private lateinit var logoutButton: Button
    private lateinit var saveButton: Button
    private lateinit var usernameText: TextView
    private lateinit var emailText: TextView
    private lateinit var venmoText: TextView
    private lateinit var imageView: ImageView
    private lateinit var takePhotoButton: Button


    // firebase and shared prefs
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mFirebaseFirestore: FirebaseFirestore
    private lateinit var mCurrUser: FirebaseUser
    private lateinit var mUserId: String
    private lateinit var pref: SharedPreferences


    private var email=""
    private var password=""


    private val cropActivityResultContract = object: ActivityResultContract<Any, Uri?>(){
        override fun createIntent(context: Context, input: Any?): Intent {
            return CropImage.activity()
                .getIntent(this@ProfileActivity)

        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return CropImage.getActivityResult(intent)?.uri
        }
    }

    private lateinit var cropActivityResultLauncher: ActivityResultLauncher<Any?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        imageView = findViewById(R.id.profileImg)

        cropActivityResultLauncher = registerForActivityResult(cropActivityResultContract){
            it?.let{ uri ->
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,uri)
                imageView.setImageBitmap(bitmap)
            }
        }

        // grab existing email and pswd
        pref = getSharedPreferences(MainActivity.MY_PREFERENCES, Context.MODE_PRIVATE)
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

        // set onclick listeners
        // logoutButton
        logoutButton = findViewById(R.id.logout_button)
        logoutButton.setOnClickListener {
            val editor: SharedPreferences.Editor = pref.edit()
            editor.putString(SignUpActivity.EMAIL_KEY, "")
            editor.putString(SignUpActivity.PASSWORD_KEY, "")
            editor.putBoolean(MainActivity.LOGGED_IN_KEY,false)
            editor.apply()

            // logout
            mFirebaseAuth.signOut()

            val intent = Intent(this, SignInSignUpActivity::class.java)
            startActivity(intent)
        }

        // saveButton
        saveButton = findViewById(R.id.save_edit_button)
        saveButton.setOnClickListener {
            val editor: SharedPreferences.Editor = pref.edit()
            editor.putString(SignUpActivity.EMAIL_KEY, email)
            editor.putString(SignUpActivity.PASSWORD_KEY, password)
            editor.putBoolean(MainActivity.LOGGED_IN_KEY,true)
            editor.apply()
        }

        // takePhotoButton
        takePhotoButton = findViewById(R.id.change_profile_photo_button)
        takePhotoButton.setOnClickListener {
            cropActivityResultLauncher.launch(null)
        }

    }

    private fun loadView() {

        // get view components
        emailText = findViewById(R.id.email_profile)
        usernameText = findViewById(R.id.username_profile)
        venmoText = findViewById(R.id.venmo_profile)

        emailText.text = email


        // firestore specific
        mFirebaseFirestore.collection("users").document(mUserId).get()
            .addOnSuccessListener {

                println("debug: $it") // debugging purposes
                usernameText.text = it.data!!["username"].toString()
                venmoText.text = it.data!!["venmo"].toString()
            }
    }
}