package com.noor.essex.urgo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SignupActivity : AppCompatActivity() {

    private lateinit var editName: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPhone: EditText
    private lateinit var editPassword: EditText
    private lateinit var editUnivName: EditText
    private lateinit var editAddress: EditText
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        initView()
        handleBackPress()
    }

    private fun handleBackPress() {
        findViewById<View>(R.id.back_arrow).setOnClickListener { finish() }
    }

    private fun initView() {
        val textToolbar = findViewById<TextView>(R.id.text_toolbar)
        textToolbar.text = "Sign up"
        editName = findViewById(R.id.edit_name)
        editEmail = findViewById(R.id.edit_email)
        editPhone = findViewById(R.id.edit_mobile)
        editPassword = findViewById(R.id.edit_password)
        editUnivName = findViewById(R.id.edit_univ_name)
        editAddress = findViewById(R.id.edit_address)
        progressBar = findViewById(R.id.progressBar)
    }

    fun validateDetails(view: View?) {
        val name = editName.text.toString()
        val email = editEmail.text.toString()
        val phone: String = editPhone.text.toString()
        val password = editPassword.text.toString()
        val address = editAddress.text.toString()
        val universityName = editUnivName.text.toString()
        if (name.isNotEmpty()) {
            if (email.isNotEmpty()) {
                if (phone.isNotEmpty()) {
                    if (phone.length == 10) {
                        if (password.isNotEmpty()) {
                            progressBar.visibility = View.VISIBLE
                            val user = User(
                                name = name,
                                email = email,
                                telephone = phone,
                                password = password,
                                address = address,
                                university = universityName,
                            )
                            registerUser(user)
                        } else {
                            editPassword.requestFocus()
                            editPassword.error = "Enter your password"
                        }
                    } else {
                        editPhone.requestFocus()
                        editPhone.error = "Enter a valid phone number"
                    }
                } else {
                    editPhone.requestFocus()
                    editPhone.error = "Enter your phone number"
                }
            } else {
                editEmail.requestFocus()
                editEmail.error = "Enter your email"
            }
        } else {
            editName.requestFocus()
            editName.error = "Enter your name"
        }
    }

    private fun registerUser(user: User) {
        FirebaseHelper.auth
            ?.createUserWithEmailAndPassword(user.email, user.password)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // returns the user id that firebase itself will generate
                    val id: String? = task.result.user?.uid
                    user.id = id
                    progressBar.let { user.saveUser(it, baseContext) }
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // handle errors
                    val error: String = FirebaseHelper.validateErrors(task.exception?.message!!)
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            }
    }

    // when you click register
    fun onClickLogin(view: View?) {
        startActivity(Intent(this, LoginActivity::class.java))
//        startActivity(Intent(this, FormSellActivity::class.java))
        finish()
    }

}