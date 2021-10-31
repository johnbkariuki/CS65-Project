package com.example.checkmate

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {
    private lateinit var logoutButton: Button
    private lateinit var mFirebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // firebase
        mFirebaseAuth = FirebaseAuth.getInstance()

        // set onclick listeners
        logoutButton = view.findViewById(R.id.logout_button)
        logoutButton.setOnClickListener {
            val pref: SharedPreferences = requireActivity().getSharedPreferences(MainActivity.MY_PREFERENCES, Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = pref.edit()
            editor.putString(SignUpActivity.EMAIL_KEY, "")
            editor.putString(SignUpActivity.PASSWORD_KEY, "")
            editor.putBoolean(MainActivity.LOGGED_IN_KEY,false)
            editor.apply()

            // logout
            mFirebaseAuth.signOut()

            val intent = Intent(requireActivity(), SignInSignUpActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}