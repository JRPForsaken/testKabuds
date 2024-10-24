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
import java.text.SimpleDateFormat
import java.util.*
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.canvas.PdfCanvas

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
    private var pdfFilePath: String? = null

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
                pdfFilePath = getRealPathFromURI(it)
                if (pdfFilePath != null) {
                    readPdfFile(pdfFilePath!!)
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
        val signeeNameText = nameInput.text.toString()
        val bitmap = signaturePad.signatureBitmap

        if (signaturePad.isEmpty || signeeNameText.isEmpty()) {
            errorText.text = "Please enter a name/signature."
        } else {
            errorText.text = ""
            printName.text = signeeNameText

            // Save the bitmap to a file instead of passing it directly
            savedBitmapPath = saveBitmapToFile(bitmap)
            if (savedBitmapPath != null && pdfFilePath != null) {
                modifyPdfWithUserInfo(pdfFilePath!!, signeeNameText, bitmap)
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

    private fun modifyPdfWithUserInfo(pdfFilePath: String, userName: String, signature: Bitmap) {
        try {
            val outputFile = File(getExternalFilesDir(null), "$userName.pdf")
            val pdfReader = PdfReader(File(pdfFilePath))
            val pdfWriter = PdfWriter(outputFile)

            // Create PdfDocument with custom options to avoid metadata parsing
            val pdfDoc = PdfDocument(pdfReader, pdfWriter)

            // Remove the XMP metadata if it exists - this should be in the document catalog
            try {
                pdfDoc.catalog.remove(PdfName.Metadata)
            } catch (e: Exception) {
                Log.e("Metadata Removal", "Failed to remove XMP metadata", e)
            }

            // Access the third page for modification
            val page = pdfDoc.getPage(3)
            val pdfCanvas = PdfCanvas(page)

            // Load the font for writing text (using Helvetica)
            val font = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA)

            // Coordinates for name, signature, and date (adjust these as per your PDF layout)
            val nameX = 150f
            val nameY = 450f
            val signatureX = 150f
            val signatureY = 400f
            val dateX = 150f
            val dateY = 350f

            // Write the name on the PDF
            pdfCanvas.beginText()
            pdfCanvas.setFontAndSize(font, 12f)
            pdfCanvas.moveText(nameX.toDouble(), nameY.toDouble())
            pdfCanvas.showText(userName)
            pdfCanvas.endText()

            // Convert the signature bitmap to an image and add it to the PDF
            val signatureImage = Image(ImageDataFactory.create(savedBitmapPath))
            signatureImage.setFixedPosition(signatureX, signatureY)
            val document = Document(pdfDoc)
            document.add(signatureImage)

            // Write the current date on the PDF
            val currentDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date())
            pdfCanvas.beginText()
            pdfCanvas.setFontAndSize(font, 12f)
            pdfCanvas.moveText(dateX.toDouble(), dateY.toDouble())
            pdfCanvas.showText(currentDate)
            pdfCanvas.endText()

            // Close the document and save changes
            document.close()
            pdfDoc.close()

            // Reload the updated PDF in the viewer
            readPdfFile(outputFile.absolutePath)
        } catch (e: Exception) {
            Log.e("PDF Modification", "Error modifying PDF", e)
            e.printStackTrace()
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
        }
    }

    override fun onStop() {
        super.onStop()
        // Clear the signature pad to avoid any bitmap-related crash during parceling
        signaturePad.clear()
    }
}
