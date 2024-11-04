package com.fantasyworld.test

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.github.gcacace.signaturepad.views.SignaturePad
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.material.snackbar.Snackbar
import android.provider.DocumentsContract
import android.view.View
import android.widget.SeekBar
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val pickPDFFile = 2001
    private val pdfFilePrefKey = "last_picked_pdf"
    //private lateinit var filePathTextView: TextView
    private lateinit var uploadButton: Button
    private lateinit var signaturePad: SignaturePad
    private lateinit var buttonClear: Button
    private lateinit var buttonSign: Button
    //private lateinit var printName: TextView
    private lateinit var nameInput: TextView
    private lateinit var errorText: TextView
    private lateinit var buttonback: Button
    private lateinit var pageSlider: SeekBar
    private lateinit var pdfView: PDFView
    private var savedBitmapPath: String? = null
    private var pdfFilePath: String? = null
    private val density by lazy { resources.displayMetrics.density }
    private var isPDFpresent = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()
        signaturePad.setSaveEnabled(false)

        // Load the last picked PDF if available
        pdfFilePath = loadLastPickedPdfPath()
        if (pdfFilePath != null) {
            readPdfFile(pdfFilePath!!)
            enableSignButton(true)
        } else {
            enableSignButton(false)
        }

        // Dynamically scale elements based on screen density
        //scaleViews()
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
    }

    private fun enableSignButton(enable: Boolean) {
        buttonSign.isEnabled = enable
        val color = if (enable) {
            ContextCompat.getColor(this, R.color.original_button_colorEN)
        } else {
            Color.GRAY
        }
        buttonSign.setBackgroundColor(color)
        Log.d("ButtonColor", "Button enabled: $enable, Color set: $color")
    }



    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
    }

    private fun initViews() {
        //filePathTextView = findViewById(R.id.filePathTextView)
        uploadButton = findViewById(R.id.uploadButton)
        signaturePad = findViewById(R.id.signature_pad)
        buttonClear = findViewById(R.id.clearButton)
        buttonSign = findViewById(R.id.signButton)
        //printName = findViewById(R.id.printname)
        nameInput = findViewById(R.id.textInputEditText)
        errorText = findViewById(R.id.errorname)
        pdfView = findViewById(R.id.pdfView)
        buttonback = findViewById(R.id.buttonback)

    }

    private fun setupClickListeners() {
        buttonSign.setOnClickListener { handleSignature() }
        buttonClear.setOnClickListener { clearSignature() }
        uploadButton.setOnClickListener { uploadPdfFile() }
        buttonback.setOnClickListener { onBackPressed() }

    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        // Start HomeActivity when the back button is pressed
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish() // Close MainActivityTagalog to prevent returning to it
    }
    //HMM
    private fun openSignedPdfDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signedPdfDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "SignedPDF")
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(signedPdfDir))
            }
        }

        try {
            val REQUEST_CODE_OPEN_DIRECTORY = 0
            startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            Snackbar.make(findViewById(android.R.id.content), "No file manager app found to open directory.", Snackbar.LENGTH_LONG).show()
        }
    }


    private fun uploadPdfFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            signaturePad.clear()
            nameInput.text = null
        }
        enableSignButton(false)  // Disable the sign button when uploading a new PDF
        startActivityForResult(intent, pickPDFFile)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickPDFFile && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            uri?.let {
                pdfFilePath = getRealPathFromURI(it)
                pdfFilePath?.let { path ->
                    saveLastPickedPdfPath(path)
                    readPdfFile(path)
                    enableSignButton(true)  // Enable the sign button after successful PDF upload
                    Snackbar.make(findViewById(android.R.id.content), "PDF uploaded successfully!", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("Recycle")
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
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(0)
                .scrollHandle(DefaultScrollHandle(this))
                .spacing((10 * density).toInt()) // Scaled spacing
                .pageFitPolicy(FitPolicy.WIDTH)
                .load()  // Add this line to load the PDF

            // Make sure the PDFView is visible
            pdfView.visibility = View.VISIBLE

            // Log success message
            Log.d("PDF Reader", "PDF loaded successfully: $filePath")
        } else {
            Log.e("PDF Reader", "File does not exist: $filePath")
            Snackbar.make(findViewById(android.R.id.content), "Error: PDF file not found", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun saveLastPickedPdfPath(path: String) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(pdfFilePrefKey, path)
            apply()
        }
    }

    private fun loadLastPickedPdfPath(): String? {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        return sharedPref.getString(pdfFilePrefKey, null)
    }

    @SuppressLint("SetTextI18n")
    private fun handleSignature() {
        val signeeNameText = nameInput.text.toString()
        val originalBitmap = signaturePad.signatureBitmap

        if (signaturePad.isEmpty || signeeNameText.isEmpty()) {
            errorText.text = "Please enter a name/signature."
        } else {
            errorText.text = ""
            //printName.text = signeeNameText

            val transparentBitmap = createTransparentBitmap(originalBitmap)
            savedBitmapPath = saveBitmapToFile(transparentBitmap)

            if (savedBitmapPath != null && pdfFilePath != null) {
                modifyPdfWithUserInfo(pdfFilePath!!, signeeNameText, savedBitmapPath!!)
                Snackbar.make(findViewById(android.R.id.content), "Signature saved successfully!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun createTransparentBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val transparentBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                if (pixel == android.graphics.Color.WHITE) {
                    transparentBitmap.setPixel(x, y, android.graphics.Color.TRANSPARENT)
                } else {
                    transparentBitmap.setPixel(x, y, pixel)
                }
            }
        }
        return transparentBitmap
    }

    private fun saveBitmapToFile(bitmap: Bitmap): String? {
        val file = File(getExternalFilesDir(null), "signature.png")
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("Signature", "Error saving bitmap", e)
            null
        }
    }

    private fun modifyPdfWithUserInfo(pdfFilePath: String, userName: String, signaturePath: String) {
        try {
            val documentsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "SignedPDF")
            if (!documentsDir.exists()) documentsDir.mkdirs()

            // Generate a unique filename by checking if files with the same name already exist
            var outputFile = File(documentsDir, "$userName-signed.pdf")
            var counter = 1
            while (outputFile.exists()) {
                outputFile = File(documentsDir, "$userName($counter)-signed.pdf")
                counter++
            }

            val pdfReader = PdfReader(File(pdfFilePath))
            val pdfWriter = PdfWriter(outputFile)
            val pdfDoc = PdfDocument(pdfReader, pdfWriter)
            pdfDoc.catalog.remove(PdfName.Metadata)

            // Initialize font
            val font = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA)
            val detectedFields = detectFieldsInPdf(pdfDoc, listOf("Name", "Sign", "Date", "signature", "date"))

            if (detectedFields.isNotEmpty()) {
                for ((field, position) in detectedFields) {
                    val pdfCanvas = PdfCanvas(pdfDoc.getPage(position.pageNumber))

                    when (field) {
                        "Name", "name", "Name:", "I am" -> {
                            pdfCanvas.beginText()
                            pdfCanvas.setFontAndSize(font, 14f)
                            val adjustedY = position.y - 12 // Adjust as needed
                            pdfCanvas.moveText((position.x + 14).toDouble(), adjustedY.toDouble())
                            pdfCanvas.showText(userName)
                            pdfCanvas.endText()
                            Log.d("PDF Modification", "Placed name at page ${position.pageNumber} at x=${position.x + 10}, y=$adjustedY")
                        }
                        "Sign", "signature", "Signature:" -> {
                            val signatureImageData = ImageDataFactory.create(signaturePath)
                            val adjustedY = position.y + 70 // Adjust as needed
                            pdfCanvas.addImage(signatureImageData, position.x + 35, adjustedY - 120, 70f, false)
                            Log.d("PDF Modification", "Placed signature at page ${position.pageNumber} at x=${position.x + 10}, y=$adjustedY")
                        }
                        "Date", "date", "Date:", "On the" -> {
                            val currentDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date())
                            pdfCanvas.beginText()
                            pdfCanvas.setFontAndSize(font, 12f)
                            val adjustedY = position.y - 70 // Adjust as needed
                            pdfCanvas.moveText((position.x + 10).toDouble(), adjustedY.toDouble())
                            pdfCanvas.showText(currentDate)
                            pdfCanvas.endText()
                            Log.d("PDF Modification", "Placed date at page ${position.pageNumber} at x=${position.x + 10}, y=$adjustedY")
                        }
                    }
                }
            }
            pdfDoc.close()
            Log.d("PDF Modification", "PDF modified and saved to $outputFile")
            Snackbar.make(findViewById(android.R.id.content), "Signed!", Snackbar.LENGTH_SHORT).show()
            signaturePad.clear()
            nameInput.text = null

        } catch (e: Exception) {
            Log.e("PDF Modification", "Error modifying PDF", e)
            Snackbar.make(findViewById(android.R.id.content), "Error modifying PDF: ${e.message}", Snackbar.LENGTH_LONG).show()
            Snackbar.make(findViewById(android.R.id.content), "Error on modifying!", Snackbar.LENGTH_SHORT).show()
        }
    }


    private fun detectFieldsInPdf(pdfDoc: PdfDocument, fields: List<String>): Map<String, Position> {
        val fieldPositions = mutableMapOf<String, Position>()
        for (i in 1..pdfDoc.numberOfPages) {
            val page = pdfDoc.getPage(i)
            val strategy = LocationTextExtractionStrategy()
            PdfCanvasProcessor(strategy).processPageContent(page)

            // Check for fields
            val text = strategy.resultantText
            fields.forEach { field ->
                if (text.contains(field, ignoreCase = true)) {
                    // Logic to calculate the position (for simplicity, assuming fixed positions)
                    val position = Position(100f, 700f, i) // Replace with actual logic to find text position
                    fieldPositions[field] = position
                }
            }
        }
        return fieldPositions
    }

    data class Position(val x: Float, val y: Float, val pageNumber: Int)



    private fun clearSignature() {
        signaturePad.clear()
        Snackbar.make(findViewById(android.R.id.content),"Cleared Signature", Snackbar.LENGTH_SHORT).show()
    }
}
