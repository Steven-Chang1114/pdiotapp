package com.specknet.pdiotapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    // buttons and textviews
    lateinit var loginBtn: Button
    lateinit var signupBtn: Button
    lateinit var email: TextView
    lateinit var password: TextView

    private lateinit var googleAuth: FirebaseAuth
    private lateinit var auth: FirebaseAuth
    lateinit var gso : GoogleSignInOptions
    lateinit var googleSignInClient: GoogleSignInClient
    lateinit var googleBtn: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.login)

        loginBtn = findViewById(R.id.login_btn)
        email = findViewById(R.id.emailField)
        password = findViewById(R.id.password)
        googleBtn = findViewById(R.id.google_btn)
        signupBtn = findViewById(R.id.signup_btn)

        googleAuth = FirebaseAuth.getInstance()
        auth = Firebase.auth

        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val firebaseAccount = auth.currentUser
        val googleAuthAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (googleAuthAccount != null) {
            Log.d("PDIOT_FIREBASE_AUTH", "Google autodirect")
            googleNavigateToMainPage(googleAuthAccount)
        } else if (firebaseAccount != null) {
            Log.d("PDIOT_FIREBASE_AUTH", "firebase autodirect")
            firebaseNavigateToMainPage(firebaseAccount)
        }

        setupClickListeners()
    }

    fun setupClickListeners() {
        loginBtn.setOnClickListener {
            auth.signInWithEmailAndPassword(email.text.toString().trim(), password.text.toString().trim())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("PDIOT_FIREBASE_AUTH", "signInWithEmail:success")
                        val user = auth.currentUser
                        if (user != null) {
                            firebaseNavigateToMainPage(user)
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("PDIOT_FIREBASE_AUTH", "signInWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, task.exception.toString(),
                            Toast.LENGTH_SHORT).show()
                    }
                }
        }

        signupBtn.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

        googleBtn.setOnClickListener {
            signInGoogle()
        }
    }

    private fun signInGoogle() {
        Log.d("PDIOT_FIREBASE_AUTH", "Google Sign in")
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
                if (result.resultCode == Activity.RESULT_OK){
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    handleResults(task)
                }
    }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful){
            val account : GoogleSignInAccount? = task.result
            if (account != null){
                updateUI(account)
            }
        }else{
            Toast.makeText(this, task.exception.toString() , Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken , null)
        googleAuth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful){
                googleNavigateToMainPage(account)
            }else{
                Toast.makeText(this, it.exception.toString() , Toast.LENGTH_SHORT).show()

            }
        }
    }

    private fun googleNavigateToMainPage(account: GoogleSignInAccount) {
        val intent = Intent(this , HomePage::class.java)
        intent.putExtra("name" , account.displayName)
        startActivity(intent)
    }

    private fun firebaseNavigateToMainPage(account: FirebaseUser) {
        val intent = Intent(this , HomePage::class.java)
        intent.putExtra("name" , account.displayName)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        System.exit(0)
    }
}