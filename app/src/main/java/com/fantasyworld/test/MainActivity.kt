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
        val fullname: EditText = findViewById(R.id.fullNameInput) // Change TextView to EditText
        val printneym: TextView = findViewById(R.id.printname)

        buttonSign.setOnClickListener {
            // Get the inputted name from the fullname EditText
            val signeeNameText = fullname.text.toString()
            // Update the TextView to show the inputted name
            printneym.text = signeeNameText

            // Capture the signature as a Bitmap
            val bitmap = signaturePad.signatureBitmap
            imagebiyu.setImageBitmap(bitmap) // Display the signature image
            //TODO: Edit text saving
        }
        buttonClear.setOnClickListener {
            signaturePad.clear()
            //TODO: Easy name clearing

        }
    }


    fun Clear(view: View) {}
}