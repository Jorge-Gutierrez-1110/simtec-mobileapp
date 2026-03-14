package com.example.simtec_mobileapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var btnLogout: Button
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        btnLogout = findViewById(R.id.btnLogout)
        sessionManager = SessionManager(this)

        btnLogout.setOnClickListener {

            sessionManager.logout()

            startActivity(Intent(this, LoginActivity::class.java))
            finish()

        }
    }
}
