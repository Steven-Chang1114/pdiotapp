package com.specknet.pdiotapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    // buttons and textviews
    lateinit var loginBtn: Button
    lateinit var username: TextView
    lateinit var password: TextView

    private lateinit var auth: FirebaseAuth
    lateinit var gso : GoogleSignInOptions
    lateinit var googleSignInClient: GoogleSignInClient
    lateinit var googleBtn: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val account = GoogleSignIn.getLastSignedInAccount(this);
//        if (account != null) {
//            navigateToMainPage(account)
//        }

        setContentView(R.layout.login)

        loginBtn = findViewById(R.id.login_btn)
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        googleBtn = findViewById(R.id.google_btn)

        auth = FirebaseAuth.getInstance()

        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setupClickListeners()
    }

    fun setupClickListeners() {
        loginBtn.setOnClickListener {
            if (username.text.toString().equals("admin") &&
                password.text.toString().equals("admin")) {
                val intent = Intent(this, HomePage::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
            }
        }

        googleBtn.setOnClickListener {
            signInGoogle()
        }
    }

    private fun signInGoogle() {
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
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful){
                navigateToMainPage(account)
            }else{
                Toast.makeText(this, it.exception.toString() , Toast.LENGTH_SHORT).show()

            }
        }
    }

    private fun navigateToMainPage(account: GoogleSignInAccount) {
        val intent = Intent(this , HomePage::class.java)
        intent.putExtra("name" , account.displayName)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        System.exit(0)
    }
}