package com.specknet.pdiotapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase

class SignUp : AppCompatActivity() {
    lateinit var usernameField: TextView
    lateinit var emailField: TextView
    lateinit var passwordField: TextView
    lateinit var signupBtn: Button
    lateinit var backBtn : ImageView

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)

        usernameField = findViewById(R.id.username)
        emailField = findViewById(R.id.email)
        passwordField = findViewById(R.id.password)
        signupBtn = findViewById(R.id.signup_btn)
        backBtn = findViewById(R.id.back_btn)

        auth = Firebase.auth

        backBtn.setOnClickListener {
            this.finish()
            val intent = Intent(this, MainActivity::class.java)

            startActivity(intent)
            overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right);
        }

        signupBtn.setOnClickListener {
            val username = usernameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            if (username != "" && email != "" && password != "") {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("PDIOT_FIREBASE_AUTH", "createUserWithEmail:success")
                            val user = auth.currentUser

                            val profileUpdates = userProfileChangeRequest {
                                displayName = username
                            }

                            user!!.updateProfile(profileUpdates)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        updateUI(user)
                                    }
                                }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("PDIOT_FIREBASE_AUTH", "createUserWithEmail:failure", task.exception)
                            Toast.makeText(baseContext, task.exception.toString(),
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(user: FirebaseUser) {
        Log.d("PDIOT_FIREBASE_AUTH", "User profile updated as " + user.displayName)
        val intent = Intent(this , HomePage::class.java)
        intent.putExtra("name" , user.displayName)
        startActivity(intent)
    }
}