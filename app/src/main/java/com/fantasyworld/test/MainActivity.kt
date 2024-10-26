package com.fantasyworld.test

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import com.itextpdf.layout.element.Image
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import android.Manifest
import android.os.Environment
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    private val pickPDFFile = 2001
    private val pdfFilePrefKey = "last_picked_pdf"
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
        signaturePad.setSaveEnabled(false)

        // Load the last picked PDF if available
        pdfFilePath = loadLastPickedPdfPath()
        if (pdfFilePath != null) {
            readPdfFile(pdfFilePath!!)
        }
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
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
                pdfFilePath = getRealPathFromURI(it)
                pdfFilePath?.let { path ->
                    saveLastPickedPdfPath(path)
                    readPdfFile(path)
                    filePathTextView.text = path
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
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(0)
                .scrollHandle(DefaultScrollHandle(this))
                .spacing(10)
                .pageFitPolicy(FitPolicy.WIDTH)
                .load()
        } else {
            Log.e("PDF Reader", "File does not exist")
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
            printName.text = signeeNameText

            val transparentBitmap = createTransparentBitmap(originalBitmap)
            savedBitmapPath = saveBitmapToFile(transparentBitmap)

            if (savedBitmapPath != null && pdfFilePath != null) {
                modifyPdfWithUserInfo(pdfFilePath!!, signeeNameText, savedBitmapPath!!)
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
            if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
                Log.e("PDF Modification", "External storage is not mounted or writable.")
                return
            }

            val documentsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "SignedPDF")
            if (!documentsDir.exists()) documentsDir.mkdirs()

            val outputFile = File(documentsDir, "$userName-signed.pdf")
            val pdfReader = PdfReader(File(pdfFilePath))
            val pdfWriter = PdfWriter(outputFile)
            val pdfDoc = PdfDocument(pdfReader, pdfWriter)
            pdfDoc.catalog.remove(PdfName.Metadata)

            val lastPage = pdfDoc.getPage(pdfDoc.numberOfPages)
            val pageSize = lastPage.pageSize
            val pdfCanvas = PdfCanvas(lastPage)
            val font = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA)

            val centerX = pageSize.width / 2
            val marginBottom = 50f

            val nameY = pageSize.bottom + marginBottom + 50f
            val dateY = pageSize.bottom + marginBottom + 30f
            val signatureY = pageSize.bottom + marginBottom + 10f

            pdfCanvas.beginText()
            pdfCanvas.setFontAndSize(font, 12f)
            pdfCanvas.moveText((centerX - 50).toDouble(), nameY.toDouble())
            pdfCanvas.showText(userName)
            pdfCanvas.endText()

            val currentDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date())
            pdfCanvas.beginText()
            pdfCanvas.setFontAndSize(font, 12f)
            pdfCanvas.moveText((centerX - 50).toDouble(), dateY.toDouble())
            pdfCanvas.showText(currentDate)
            pdfCanvas.endText()

            val signatureImageData = ImageDataFactory.create(signaturePath)
            pdfCanvas.addImage(
                signatureImageData,
                centerX - (150f / 2),
                signatureY,
                150f,
                false
            )

            pdfDoc.close()
            signaturePad.clear()
            nameInput.text = null

            readPdfFile(outputFile.absolutePath)
        } catch (e: Exception) {
            Log.e("PDF Modification", "Error modifying PDF", e)
        }
    }

    private fun clearSignature() {
        signaturePad.clear()
        nameInput.text = null
    }
}
