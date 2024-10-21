package com.fantasyworld.test

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.barteksc.pdfviewer.PDFView
import com.github.gcacace.signaturepad.views.SignaturePad
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy

class MainActivity : AppCompatActivity() {
    private val pickPDFFile = 2001
    private lateinit var filePathTextView: TextView
    private lateinit var uploadButton: Button
    private lateinit var signaturePad: SignaturePad
    private lateinit var buttonClear: Button
    private lateinit var buttonSign: Button
    private lateinit var printName: TextView
    private lateinit var nameInput: TextView
    private lateinit var errorText: TextView
    private lateinit var pdfView: PDFView
    private var savedBitmapPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()

        // Disable state saving for SignaturePad to prevent bitmap parcel issues
        signaturePad.setSaveEnabled(false)
    }

    private fun initViews() {
        filePathTextView = findViewById(R.id.filePathTextView)
        uploadButton = findViewById(R.id.uploadButton)
        signaturePad = findViewById(R.id.signature_pad)
        buttonClear = findViewById(R.id.clearButton)
        buttonSign = findViewById(R.id.signButton)
        printName = findViewById(R.id.printname)
        nameInput = findViewById(R.id.textInputEditText)
        errorText = findViewById(R.id.errorname)
        pdfView = findViewById(R.id.pdfView)
    }

    private fun setupClickListeners() {
        buttonSign.setOnClickListener { handleSignature() }
        buttonClear.setOnClickListener { clearSignature() }
        uploadButton.setOnClickListener { uploadPdfFile() }
    }

    private fun uploadPdfFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
        }
        startActivityForResult(intent, pickPDFFile)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickPDFFile && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            uri?.let {
                filePathTextView.text = it.path
                val pdfPath = getRealPathFromURI(it)
                if (pdfPath != null) {
                    readPdfFile(pdfPath)
                } else {
                    Log.e("PDF Reader", "Failed to get the file path from URI")
                }
            }
        }
    }

    private fun getRealPathFromURI(contentUri: Uri): String? {
        val fileDescriptor = contentResolver.openFileDescriptor(contentUri, "r") ?: return null
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val file = File(cacheDir, "tempFile.pdf")
        FileOutputStream(file).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()
        return file.absolutePath
    }

    private fun readPdfFile(filePath: String) {
        val pdfFile = File(filePath)
        if (pdfFile.exists()) {
            pdfView.fromFile(pdfFile)
                .enableSwipe(true) // enables horizontal swipe
                .swipeHorizontal(false) // set false for vertical scrolling
                .enableDoubletap(true)
                .defaultPage(0) // start from the first page
                .scrollHandle(DefaultScrollHandle(this)) // add a scroll bar handle
                .spacing(10) // space between pages
                .pageFitPolicy(FitPolicy.WIDTH) // fit pages to the width of the view
                .load()
        } else {
            Log.e("PDF Reader", "File does not exist")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handleSignature() {
        val signeeNameText = nameInput.text
        val bitmap = signaturePad.signatureBitmap

        if (signaturePad.isEmpty || signeeNameText.isEmpty()) {
            errorText.text = "Please enter a name/signature."
        } else {
            errorText.text = ""
            printName.text = signeeNameText

            // Save the bitmap to a file instead of passing it directly
            savedBitmapPath = saveBitmapToFile(bitmap)
            if (savedBitmapPath != null) {
                // Optionally, you can display the saved bitmap in an ImageView
                // imageSign.setImageBitmap(BitmapFactory.decodeFile(savedBitmapPath))
            }
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): String? {
        val file = File(getExternalFilesDir(null), "signature.png")
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath // Return the file path
        } catch (e: Exception) {
            Log.e("Signature", "Error saving bitmap", e)
            null
        }
    }

    private fun clearSignature() {
        signaturePad.clear()
        printName.text = null
        errorText.text = null
        nameInput.text = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Disable saving the SignaturePad state
        outState.putBoolean("signature_pad_state", false)
        // Save only essential data, e.g., the name input
        outState.putString("nameInput", nameInput.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore essential data
        nameInput.setText(savedInstanceState.getString("nameInput", ""))
        // Load the saved bitmap from the file
        if (savedBitmapPath != null) {
            val bitmap = BitmapFactory.decodeFile(savedBitmapPath)
            // Optionally, you can display the loaded bitmap in an ImageView
            // imageSign.setImageBitmap(bitmap)
        }
    }

    override fun onStop() {
        super.onStop()
        // Clear the signature pad to avoid any bitmap-related crash during parceling
        signaturePad.clear()
    }
}
