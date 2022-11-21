package com.specknet.pdiotapp.demo

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.specknet.pdiotapp.R

class HistoricalData : AppCompatActivity() {

    lateinit var userId : String
    lateinit var db : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.historical_data)

        db = Firebase.firestore
        getUserId()
    }

    private fun readData() {
        db.collection(userId)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    Log.d("PDIOT_DB", "${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("PDIOT_DB", "Error getting documents.", exception)
            }
    }

    private fun getUserId() {
        val auth = Firebase.auth
        val firebaseAccount = auth.currentUser
        val googleAuthAccount = GoogleSignIn.getLastSignedInAccount(this)

        if (googleAuthAccount != null) {
            userId = googleAuthAccount.email.toString()
        } else if (firebaseAccount != null) {
            userId = firebaseAccount.uid
        }

        Log.d("PDIOT_FIREBASE_DEMO", userId)
    }
}