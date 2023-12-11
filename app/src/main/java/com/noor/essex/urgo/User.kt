package com.noor.essex.urgo

import android.content.Context
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.gms.tasks.Task

data class User(
    var id: String? = null,
    val name: String,
    val email: String,
    val telephone: String,
    val password: String,
    val address: String,
    val university: String,
    val profileImage: String? = null
) {
    fun saveUser(progressBar: ProgressBar, context: Context?) {
        val dbRef = FirebaseHelper.databaseReference
        dbRef?.child("users")
            ?.child(this.id!!)
            ?.setValue(this)?.addOnCompleteListener { task: Task<Void?> ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context,
                        "Upload error, please try again later.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                progressBar.visibility = View.GONE
            }
    }
}
