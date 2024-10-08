package com.fantasyworld.test

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.gcacace.signaturepad.views.SignaturePad
import org.w3c.dom.Text


class MainActivity : AppCompatActivity() {


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        var signeeName: TextView? = null
        val signaturePad: SignaturePad = findViewById(R.id.signature_pad)
        val buttonClear: Button = findViewById(R.id.clearButton)
        val buttonSign: Button = findViewById(R.id.signButton)
        val imagebiyu: ImageView = findViewById(R.id.imagesignatura)
        val printneym: TextView = findViewById(R.id.printname)
        val name: TextView = findViewById(R.id.textInputEditText)
        val errortext: TextView = findViewById(R.id.errorname)

        buttonSign.setOnClickListener {
            // Get the inputted name from the fullname EditText
            val signeeNameText = name.text
            // Update the TextView to show the inputted name
            printneym.text = signeeNameText

            // Capture the signature as a Bitmap
            val bitmap = signaturePad.signatureBitmap
            //imagebiyu.setImageBitmap(bitmap) // Display the signature image
            //TODO: Edit text saving
            //printneym.text = name.text

            if (signaturePad.isEmpty || signeeNameText.isEmpty()) {
                errortext.text = "Please enter a name/signature."
            } else {
                errortext.text = ""
                printneym.text = name.text
                imagebiyu.setImageBitmap(bitmap)
            }
        }
        buttonClear.setOnClickListener {
            signaturePad.clear() // Clear the signature Pad
            imagebiyu.setImageBitmap(null)
            printneym.text = null // Clear the displayed name as well
            errortext.text = null
            name.text = null
        }
    }
    //TODO LIST PDF

    fun Clear(view: View) {}
}
