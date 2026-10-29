package com.noisequest.webwrapper

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class UrlSetupActivity : AppCompatActivity() {
    
    private lateinit var urlEditText: TextInputEditText
    private lateinit var scanQrButton: Button
    private lateinit var saveButton: Button
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scanQRCode()
        } else {
            Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val scannedUrl = result.contents
            val urlToSet = extractUrl(scannedUrl)
            urlEditText.setText(urlToSet)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_url_setup)
        
        urlEditText = findViewById(R.id.urlEditText)
        scanQrButton = findViewById(R.id.scanQrButton)
        saveButton = findViewById(R.id.saveButton)
        
        // Load existing URL if any
        val prefs = getSharedPreferences("NoisequestPrefs", MODE_PRIVATE)
        val existingUrl = prefs.getString("instance_url", "")
        if (!existingUrl.isNullOrEmpty()) {
            urlEditText.setText(existingUrl)
        }
        
        scanQrButton.setOnClickListener {
            checkCameraPermissionAndScan()
        }
        
        saveButton.setOnClickListener {
            val url = urlEditText.text.toString().trim()
            if (isValidUrl(url)) {
                val normalizedUrl = normalizeUrl(url)
                val editor = prefs.edit()
                editor.putString("instance_url", normalizedUrl)
                editor.apply()
                
                Toast.makeText(this, R.string.url_saved, Toast.LENGTH_SHORT).show()
                
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun checkCameraPermissionAndScan() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                scanQRCode()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun scanQRCode() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan a QR code with the instance URL")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(false)
        options.setOrientationLocked(true)
        options.captureActivity = CaptureActivityPortrait::class.java
        barcodeLauncher.launch(options)
    }
    
    private fun extractUrl(text: String): String {
        val matcher = Patterns.WEB_URL.matcher(text)
        return if (matcher.find()) {
            matcher.group()
        } else {
            text
        }
    }
    
    private fun isValidUrl(url: String): Boolean {
        return url.isNotEmpty() && Patterns.WEB_URL.matcher(url).matches()
    }
    
    private fun normalizeUrl(url: String): String {
        return if (url.endsWith("/")) {
            url.dropLast(1)
        } else {
            url
        }
    }
}
