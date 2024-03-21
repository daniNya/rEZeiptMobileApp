package com.example.rezeipt

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.io.File

private var mUploadMessage: ValueCallback<Uri>? = null
private var uploadMessage: ValueCallback<Array<Uri>>? = null

const val FILECHOOSER_RESULTCODE = 1
const val REQUEST_SELECT_FILE = 100

class MainActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val webView = findViewById<WebView>(R.id.webview)
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.SwipeContainer)

        webView.settings.javaScriptEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.settings.domStorageEnabled = true
        webView.loadUrl("https://rezeipt.xyz/")
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                Log.d("MainActivity", "onPermissionRequest")
                    ActivityResultContracts.RequestPermission()
            }
            // For Android 3.0+
            fun openFileChooser(uploadMsg: ValueCallback<*>, acceptType: String) {
                mUploadMessage = uploadMsg as ValueCallback<Uri>
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "*/*"
                this@MainActivity.startActivityForResult(
                    Intent.createChooser(i, "File Browser"),
                    FILECHOOSER_RESULTCODE)
            }

            //For Android 4.1
            fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String, capture: String) {
                mUploadMessage = uploadMsg
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "image/*"
                this@MainActivity.startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE)

            }

            protected fun openFileChooser(uploadMsg: ValueCallback<Uri>) {
                mUploadMessage = uploadMsg
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "*/*"
                startActivityForResult(Intent.createChooser(intent, "File Chooser"), FILECHOOSER_RESULTCODE)
            }

            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                uploadMessage?.onReceiveValue(null)
                uploadMessage = null

                uploadMessage = filePathCallback

                val intent = fileChooserParams!!.createIntent()
                try {
                    startActivityForResult(intent, REQUEST_SELECT_FILE)
                } catch (e: ActivityNotFoundException) {
                    uploadMessage = null
                    Toast.makeText(applicationContext, "Cannot Open File Chooser", Toast.LENGTH_LONG).show()
                    return false
                }

                return true
            }

        }

        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode === REQUEST_SELECT_FILE) {
                if (uploadMessage == null)
                    return
                print("result code = " + resultCode)
                var results: Array<Uri>? = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                uploadMessage?.onReceiveValue(results)
                uploadMessage = null
            }
        } else if (requestCode === FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage)
                return
            // Use MainActivity.RESULT_OK if you're implementing WebView inside Fragment
            // Use RESULT_OK only if you're implementing WebView inside an Activity
            val result = if (intent == null || resultCode !== RESULT_OK) null else intent.data
            mUploadMessage?.onReceiveValue(result)
            mUploadMessage = null
        } else
            Toast.makeText(applicationContext, "Failed to Upload Image", Toast.LENGTH_LONG).show()
    }
}