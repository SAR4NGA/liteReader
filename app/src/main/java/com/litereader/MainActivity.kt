package com.litereader

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.litereader.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView
    private var pendingDocxBytes: ByteArray? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { openFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        webView = binding.webView
        setupWebView()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri -> openFile(uri) }
            }
            Intent.ACTION_MAIN -> {
                if (pendingDocxBytes == null && webView.url != null) {
                    pickFile()
                }
            }
        }
    }

    private fun openFile(uri: Uri) {
        try {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw Exception("Could not read file")

            if (::webView.isInitialized && webView.url != null && webView.progress == 100) {
                loadDocx(bytes)
            } else {
                pendingDocxBytes = bytes
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            domStorageEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
        }
        webView.isHorizontalScrollBarEnabled = true
        webView.isVerticalScrollBarEnabled = true
        webView.overScrollMode = android.view.View.OVER_SCROLL_ALWAYS

        webView.addJavascriptInterface(DocxBridge(), "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                pendingDocxBytes?.let { bytes ->
                    loadDocx(bytes)
                    pendingDocxBytes = null
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean = false
        }

        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun loadDocx(bytes: ByteArray) {
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        webView.evaluateJavascript("loadDocument('$base64')", null)
    }

    private fun pickFile() {
        filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    }

    inner class DocxBridge {
        @JavascriptInterface
        fun pickFile() {
            runOnUiThread { this@MainActivity.pickFile() }
        }

        @JavascriptInterface
        fun setDarkMode(enabled: Boolean) {
            getSharedPreferences("litereader", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("dark_mode", enabled)
                .apply()
        }

        @JavascriptInterface
        fun isDarkMode(): Boolean {
            return getSharedPreferences("litereader", Context.MODE_PRIVATE)
                .getBoolean("dark_mode", false)
        }
    }

}
