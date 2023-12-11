package com.noor.essex.urgo

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private var editEmail: EditText? = null
    private var editPassword: EditText? = null
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        intiView()
        handleBackPress()
    }

    fun validateAuth(view: View?) {
        val email = editEmail!!.text.toString()
        val password = editPassword!!.text.toString()
        if (email.isNotEmpty()) {
            if (password.isNotEmpty()) {
                progressBar!!.visibility = View.VISIBLE
                login(email, password)
            } else {
                editPassword!!.requestFocus()
                editPassword!!.error = "Enter your password."
            }
        } else {
            editEmail!!.requestFocus()
            editEmail!!.error = "Enter your email."
        }
    }

    // when you click register
    fun onClickSignup(view: View?) {
        startActivity(Intent(this, SignupActivity::class.java))
    }

    // When you click forgot password?
    fun onForgotPasswordClicked(view: View?) {
//        startActivity(Intent(this, RecuperarSenhaActivity::class.java))
    }

    private fun login(email: String, password: String) {
        FirebaseHelper.auth?.signInWithEmailAndPassword(
            email, password
        )?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                // handle errors
                val error: String? =
                    task.exception?.message?.let { FirebaseHelper.validateErrors(it) }
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
            progressBar!!.visibility = View.GONE
        }
    }

    private fun intiView() {
        val textToolbar = findViewById<TextView>(R.id.text_toolbar)
        textToolbar.text = "Login"
        editEmail = findViewById(R.id.edit_email)
        editPassword = findViewById(R.id.edit_password)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun handleBackPress() {
        findViewById<View>(R.id.back_arrow).setOnClickListener { onBackPressed() }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setMessage("Are you sure to quit the app?")
            .setPositiveButton("Yes") { _, _ ->
                finish()
            }
            .setNegativeButton("No") { _, _ ->

            }
            .show()
    }
}