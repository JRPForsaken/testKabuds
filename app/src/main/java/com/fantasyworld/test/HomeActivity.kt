package com.fantasyworld.test
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        // Initialize the button and set an onClickListener
        val openPdfButton: Button = findViewById(R.id.button2)
        openPdfButton.setOnClickListener {
            // Launch the PDF signer activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}