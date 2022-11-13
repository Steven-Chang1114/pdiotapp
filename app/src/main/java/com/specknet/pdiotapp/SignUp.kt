package com.specknet.pdiotapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SignUp : AppCompatActivity() {
    lateinit var username: TextView
    lateinit var password: TextView
    lateinit var signupBtn: Button

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)

        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        signupBtn = findViewById(R.id.signup_btn)

        auth = Firebase.auth

        signupBtn.setOnClickListener {
            val name = username.text.toString().trim()
            val pas = password.text.toString().trim()
            if (name != "" && pas != "") {
                auth.createUserWithEmailAndPassword(name, pas)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("FIREBASE_AUTH", "createUserWithEmail:success")
                            val user = auth.currentUser
                            if (user != null) {
                                updateUI(user)
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("FIREBASE_AUTH", "createUserWithEmail:failure", task.exception)
                            Toast.makeText(baseContext, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(user: FirebaseUser) {
        val intent = Intent(this , HomePage::class.java)
        intent.putExtra("name" , user.displayName)
        startActivity(intent)
    }
}