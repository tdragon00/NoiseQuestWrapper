package com.noisequest.webwrapper

import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private var baseUrl: String? = null
    private var hasFailed = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        
        val prefs = getSharedPreferences("NoisequestPrefs", MODE_PRIVATE)
        baseUrl = prefs.getString("instance_url", null)
        
        if (baseUrl.isNullOrEmpty()) {
            Toast.makeText(this, R.string.no_url_set, Toast.LENGTH_LONG).show()
            goToSetup()
            return
        }
        
        setupWebView()
        loadUrl(baseUrl!!)
        
        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack() && !hasFailed) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_change_instance -> {
                showChangeInstanceDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showChangeInstanceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Change Instance")
            .setMessage("Do you want to change to a different Quest Board instance?")
            .setPositiveButton("Yes") { _, _ ->
                goToSetup("Enter a new instance URL")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess = false
            allowContentAccess = false
        }
        
        // Enable cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                // Stay within the same instance
                if (baseUrl != null && url.startsWith(baseUrl!!)) {
                    return false
                }
                return false
            }
            
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                hasFailed = false
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }
            
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (request?.isForMainFrame == true && !hasFailed) {
                        hasFailed = true
                        goToSetup("Failed to load instance. Please check the URL and try again.")
                    }
                }
            }
            
            @Deprecated("Deprecated in API level 23")
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                if (failingUrl != null && baseUrl != null && failingUrl.startsWith(baseUrl!!) && !hasFailed) {
                    hasFailed = true
                    goToSetup("Failed to load instance. Please check the URL and try again.")
                }
            }
            
            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true && !hasFailed) {
                    val statusCode = errorResponse?.statusCode ?: 0
                    if (statusCode >= 400) {
                        hasFailed = true
                        goToSetup("Instance returned an error ($statusCode). Please enter a new URL.")
                    }
                }
            }
            
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.cancel()
                if (!hasFailed) {
                    hasFailed = true
                    goToSetup("Invalid or untrusted certificate. Please use a valid HTTPS URL.")
                }
            }
        }
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                } else {
                    progressBar.visibility = View.VISIBLE
                }
            }
        }
    }
    
    private fun loadUrl(url: String) {
        webView.loadUrl(url)
    }
    
    private fun goToSetup(message: String = "Instance unavailable. Please enter a new URL.") {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            val intent = Intent(this, UrlSetupActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    override fun onResume() {
        super.onResume()
        webView.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        webView.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}
