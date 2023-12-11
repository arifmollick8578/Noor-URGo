package com.noor.essex.urgo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

object FirebaseHelper {
    var auth: FirebaseAuth? = null
        get() {
            if (field == null) {
                field = FirebaseAuth.getInstance()
            }
            return field
        }
        private set

    var databaseReference: DatabaseReference? = null
        get() {
            if (field == null) {
                field = FirebaseDatabase.getInstance().reference
            }
            return field
        }
        private set

    var storageReference: StorageReference? = null
        get() {
            if (field == null) {
                field = FirebaseStorage.getInstance().reference
            }
            return field
        }
        private set

    val authId: String?
        get() = auth!!.uid
    val isAuthenticated: Boolean
        get() = auth!!.currentUser != null

    fun validateErrors(error: String): String {
        var message = "Credentials not matched!"
        if (error.contains("There is no user record corresponding to this identifier")) {
            message = "No accounts were found with this email!"
        } else if (error.contains("The email address is badly formatted")) {
            message = "Enter a valid email!"
        } else if (error.contains("The password is invalid or the user does not have a password")) {
            message = "Invalid password, try again!"
        } else if (error.contains("The email address is already in use by another account")) {
            message = "This email is already in use."
        } else if (error.contains("Password should be at least 6 characters")) {
            message = "Enter a password with at least 6 characters"
        }
        return message
    }
}
