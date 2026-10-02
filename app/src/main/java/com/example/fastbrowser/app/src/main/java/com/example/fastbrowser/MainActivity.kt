package com.example.fastbrowser

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import org.json.JSONObject

class MainActivity : Activity() {

    private lateinit var ui: WebView    // HTML address bar
    private lateinit var page: WebView  // actual web pages

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        ui = WebView(this)
        page = WebView(this)

        root.addView(
            ui,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (56 * density).toInt())
        )
        root.addView(
            page,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        setContentView(root)

        ui.settings.javaScriptEnabled = true
        ui.addJavascriptInterface(Bridge(), "Android")
        ui.loadUrl("file:///android_asset/ui.html")

        page.settings.javaScriptEnabled = true
        page.settings.domStorageEnabled = true
        page.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                sendToUi("setUrl", url)
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                sendToUi("setUrl", url)
            }
        }
        page.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                ui.evaluateJavascript("setProgress($newProgress)", null)
            }
        }

        page.loadUrl("https://www.google.com")
    }

    private fun sendToUi(fn: String, value: String) {
        ui.evaluateJavascript("$fn(${JSONObject.quote(value)})", null)
    }

    private fun toUrl(input: String): String {
        val text = input.trim()
        return when {
            text.startsWith("http://") || text.startsWith("https://") -> text
            text.contains(".") && !text.contains(" ") -> "https://$text"
            else -> "https://www.google.com/search?q=" + Uri.encode(text)
        }
    }

    inner class Bridge {
        @JavascriptInterface
        fun go(input: String) {
            runOnUiThread { page.loadUrl(toUrl(input)) }
        }

        @JavascriptInterface
        fun back() {
            runOnUiThread { if (page.canGoBack()) page.goBack() }
        }

        @JavascriptInterface
        fun forward() {
            runOnUiThread { if (page.canGoForward()) page.goForward() }
        }

        @JavascriptInterface
        fun reload() {
            runOnUiThread { page.reload() }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (page.canGoBack()) page.goBack() else super.onBackPressed()
    }
}
