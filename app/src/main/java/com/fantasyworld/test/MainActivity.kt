package com.fantasyworld.test

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.gcacace.signaturepad.views.SignaturePad

class MainActivity : AppCompatActivity() {

    private val storagePermissionCode = 1001
    private val pickPDFFile = 2001
    private lateinit var filePathTextView: TextView
    private lateinit var uploadButton: Button
    private val contractDirectory = "Documents/contracts"

    // Views
    private lateinit var signaturePad: SignaturePad
    private lateinit var buttonClear: Button
    private lateinit var buttonSign: Button
    private lateinit var imageSign: ImageView
    private lateinit var printName: TextView
    private lateinit var nameInput: TextView
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Ensure this is your updated XML layout

        initViews()
        setupClickListeners()
    }


    private fun initViews() {
        filePathTextView = findViewById(R.id.filePathTextView)
        uploadButton = findViewById(R.id.uploadButton)
        signaturePad = findViewById(R.id.signature_pad)
        buttonClear = findViewById(R.id.clearButton)
        buttonSign = findViewById(R.id.signButton)
        imageSign = findViewById(R.id.imagesignatura)
        printName = findViewById(R.id.printname)
        nameInput = findViewById(R.id.textInputEditText)
        errorText = findViewById(R.id.errorname)
    }


    private fun setupClickListeners() {
        buttonSign.setOnClickListener { handleSignature() }
        buttonClear.setOnClickListener { clearSignature() }
        uploadButton.setOnClickListener { uploadPdfFile() }
    }


    private fun uploadPdfFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        startActivityForResult(intent, pickPDFFile)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickPDFFile && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            filePathTextView.text = uri?.path
        }
    }


    private fun queryPdfFiles() {
        val resolver: ContentResolver = contentResolver
        val uri: Uri = MediaStore.Files.getContentUri("external")
        val cursor = resolver.query(uri, null, null, null, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val columnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                    if (columnIndex >= 0) {
                        val pdfPath = cursor.getString(columnIndex)
                        readPdfFile(pdfPath)
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
    }

    // Read the selected PDF file
    private fun readPdfFile(pdfPath: String) {
        // Implement your PDF file reading logic here
        // For example, you can use a library like PdfDocument or AndroidPdf to read the PDF file
    }

    // Handle signature submission
    @SuppressLint("SetTextI18n")
    private fun handleSignature() {
        val signeeNameText = nameInput.text
        val bitmap = signaturePad.signatureBitmap

        if (signaturePad.isEmpty || signeeNameText.isEmpty()) {
            errorText.text = "Please enter a name/signature."
        } else {
            errorText.text = ""
            printName.text = signeeNameText

            // Scale down the bitmap to reduce memory usage
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 100, true) // Adjust width and height as needed
            imageSign.setImageBitmap(scaledBitmap)
        }
    }

    // Clear signature and reset UI
    private fun clearSignature() {
        signaturePad.clear()
        imageSign.setImageBitmap(null)
        printName.text = null
        errorText.text = null
        nameInput.text = null
    }

    private fun clearResources() {
        imageSign.setImageBitmap(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        clearResources()
        signaturePad.clear()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.clear()
    }
    //TODO: implement PDF view, Write and Save at /ContractWaiver/SignedPDFs

}
