package com.fantasyworld.test

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.gcacace.signaturepad.views.SignaturePad
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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

    // Initialize views
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

    // Set up button click listeners
    private fun setupClickListeners() {
        buttonSign.setOnClickListener { handleSignature() }
        buttonClear.setOnClickListener { clearSignature() }
        uploadButton.setOnClickListener {
            if (checkStoragePermissions()) openFilePicker()
        }
    }
    
    // Check if storage permissions are granted
    private fun checkStoragePermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // For Android 13+, check for the new permission
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                // For earlier versions, check the old permission
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }.also { isGranted ->
            if (!isGranted) requestStoragePermission()
        }
    }

    // Request storage permission
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), storagePermissionCode)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), storagePermissionCode)
        }
    }

    // Handle result of permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == storagePermissionCode && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePicker()
            } else {
                Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Open file picker to choose a PDF
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, pickPDFFile)
    }

    // Handle result of file picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickPDFFile && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                Log.d("MainActivity", "Selected file Uri: $uri")
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                val fileName = getFileNameFromUri(uri)
                filePathTextView.text = fileName
                Log.d("MainActivity", "Selected file name: $fileName")

                saveFileToContractsFolder(uri, fileName)
                renderPdf(uri)
            }
        }
    }

    // Get file name from Uri
    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }

    // Save selected PDF file to "Documents/contracts"
    private fun saveFileToContractsFolder(uri: Uri, fileName: String) {
        val destinationDir = File(getExternalFilesDir(null), contractDirectory)
        if (!destinationDir.exists()) destinationDir.mkdirs()

        val destinationFile = File(destinationDir, fileName)

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d("MainActivity", "File saved successfully to: ${destinationFile.absolutePath}")
            Toast.makeText(this, "File uploaded to Contracts folder", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Log.e("MainActivity", "Error saving file: ${e.message}")
            Toast.makeText(this, "Error saving file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderPdf(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val fileDescriptor: ParcelFileDescriptor? = contentResolver.openFileDescriptor(uri, "r")

            if (fileDescriptor != null) {
                var pdfRenderer: PdfRenderer? = null
                var page: PdfRenderer.Page? = null

                try {
                    pdfRenderer = PdfRenderer(fileDescriptor)

                    if (pdfRenderer.pageCount > 0) {
                        page = pdfRenderer.openPage(0)

                        val width = page.width
                        val height = page.height
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        imageSign.setImageBitmap(bitmap)
                        Log.d("MainActivity", "PDF rendered successfully")
                    } else {
                        Toast.makeText(this, "PDF has no pages", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    Log.e("MainActivity", "Error rendering PDF: ${e.message}")
                    Toast.makeText(this, "Error rendering PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    page?.close()
                    pdfRenderer?.close()
                }
            } else {
                Toast.makeText(this, "Error opening PDF file", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "PDF rendering not supported on this device", Toast.LENGTH_SHORT).show()
        }
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
    }
}
