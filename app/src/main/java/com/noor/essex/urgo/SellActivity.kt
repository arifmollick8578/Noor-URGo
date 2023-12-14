package com.noor.essex.urgo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date


class SellActivity : AppCompatActivity() {
    private lateinit var toolbarText: TextView
    private lateinit var imageRv: RecyclerView
    private lateinit var editName: EditText
    private lateinit var editCategory: EditText
    private lateinit var editCondition: EditText
    private lateinit var editPrice: EditText
    private lateinit var submitButton: Button

    private val uriItems = mutableListOf<Uri>(
        Uri.parse("any")
    )

    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sell)

        toolbarText = findViewById(R.id.text_toolbar)
        toolbarText.text = "Sell"

        imageRv = findViewById(R.id.image_rv)
        editName = findViewById(R.id.edit_name)
        editCategory = findViewById(R.id.edit_category)
        editCondition = findViewById(R.id.edit_condition)
        editPrice = findViewById(R.id.edit_price)
        submitButton = findViewById(R.id.submit_button)

        val adapter = SellImageAdapter(uriItems)
        imageRv.adapter = adapter
        imageRv.layoutManager = LinearLayoutManager(
            this,
            RecyclerView.HORIZONTAL,
            false
        )

        // click listener for camera
        submitButton.setOnClickListener {
            val modalBottomSheet = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)
            val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
            bottomSheetDialog.setContentView(modalBottomSheet)
            bottomSheetDialog.show()

            modalBottomSheet.findViewById<View>(R.id.btn_camera).setOnClickListener { v: View? ->
                bottomSheetDialog.dismiss()
                verifyCameraPermission()
            }

            modalBottomSheet.findViewById<View>(R.id.btn_galeria).setOnClickListener { v: View? ->
                bottomSheetDialog.dismiss()
                verifyGalleryPermission()
            }

            modalBottomSheet.findViewById<View>(R.id.btn_close).setOnClickListener { v: View? ->
                bottomSheetDialog.dismiss()
            }
        }
    }

    private fun verifyCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 20)
        } else {
            dispatchTakePictureIntent()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 20) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show()
                dispatchTakePictureIntent()
            } else {
                Snackbar.make(
                    submitButton,
                    "Please  grant camera permission to capture images",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        } else if (requestCode == 30) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Gallery permission granted", Toast.LENGTH_LONG).show()
                fetchImagesFromGallery()
            } else {
                Snackbar.make(
                    submitButton,
                    "Please grant files permission to select photos",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun verifyGalleryPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                30
            )
        } else {
            fetchImagesFromGallery()
        }
    }

    private fun fetchImagesFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Pictures"), 150)
    }

    fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Create the File where the photo should go
        var photoFile: File? = null
        try {
            photoFile = createImageFile()
        } catch (ex: IOException) {
            // Error occurred while creating the File
        }
        // Continue only if the File was successfully created
        if (photoFile != null) {
            val photoURI = FileProvider.getUriForFile(
                this,
                "com.noor.essex.urgo.fileprovider",
                photoFile
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            takePictureIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(takePictureIntent, 100)
        }
    }

    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File? {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.absolutePath
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var imageEncoded: String? = null
        val imagesEncodedList: ArrayList<String> = ArrayList()

        if (resultCode == RESULT_OK) {
            val selectedImage = data!!.data
            if (requestCode == 100) {
                val file = currentPhotoPath?.let { File(it) }
                val uri = Uri.fromFile(file)
                uriItems.add(uri)
                imageRv.adapter = SellImageAdapter(uriItems)
            } else if (requestCode == 150) { // Galeria
                val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                if (selectedImage != null) {
                    uriItems.add(selectedImage)
                } else {
                    if (data.clipData != null) {
                        val mClipData = data.clipData
                        val mArrayUri = ArrayList<Uri>()
                        for (i in 0 until mClipData?.itemCount!!) {
                            val item = mClipData.getItemAt(i)
                            val uri = item.uri
                            mArrayUri.add(uri)
                            // Get the cursor
                            val cursor =
                                contentResolver.query(uri, filePathColumn, null, null, null)
                            // Move to first row
                            cursor?.moveToFirst()
                            uriItems.add(uri)
                            cursor?.close()
                        }
                    }
                }

                imageRv.adapter = SellImageAdapter(uriItems)

            } else {
                Toast.makeText(
                    this, "You haven't picked Image",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else { // Camera
        }
    }
}