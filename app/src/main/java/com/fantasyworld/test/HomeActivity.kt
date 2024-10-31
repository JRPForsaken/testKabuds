package com.fantasyworld.test
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)
        // Waiver button to open MainActivity
        val waiverButton: Button = findViewById(R.id.button2)
        waiverButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        // Family Waiver button to open MainActivityTagalog
        val familyWaiverButton: Button = findViewById(R.id.button3)
        familyWaiverButton.setOnClickListener {
            val intent = Intent(this, MainActivityTagalog::class.java)
            startActivity(intent)
        }
    }
}