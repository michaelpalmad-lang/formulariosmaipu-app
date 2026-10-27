package cl.easymaipu.formularios

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.WebResourceRequest
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

/**
 * Portal de formularios Easy Maipú.
 * - El portal va incluido en la app (assets/index.html): no depende de internet ni de Netlify.
 * - Para cambiar el contenido se reemplaza assets/index.html y se compila un APK nuevo.
 * - Los formularios y enlaces externos se abren en el navegador del teléfono.
 * - El PDF de Mi Local para PDA se abre con el visor de PDF del teléfono.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView

    companion object {
        const val HOME = "file:///android_asset/index.html"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        web = WebView(this).apply { setBackgroundColor(Color.parseColor("#1c1f22")) }
        setContentView(web)

        web.addJavascriptInterface(AppBridge(), "AndroidApp")
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                val url = uri.toString()
                return when {
                    url.startsWith("data:application/pdf") -> { openPdf(url); true }
                    uri.scheme == "file" -> false
                    else -> { openExternal(uri); true }
                }
            }

        }

        // Los enlaces con atributo "download" (el PDF embebido) llegan por aquí.
        web.setDownloadListener { url, _, _, _, _ ->
            if (url.startsWith("data:application/pdf")) openPdf(url)
            else openExternal(Uri.parse(url))
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) {
                    web.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        web.loadUrl(HOME)
    }

    /** Puente para que las pantallas locales copien texto al portapapeles de Android. */
    inner class AppBridge {
        @JavascriptInterface
        fun copy(text: String) {
            runOnUiThread {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Reporte", text))
            }
        }
    }

    private fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPdf(dataUrl: String) {
        try {
            val base64 = dataUrl.substringAfter("base64,")
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val dir = File(cacheDir, "pdfs").apply { mkdirs() }
            val file = File(dir, "Mi_Local_para_PDA.pdf")
            file.writeBytes(bytes)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Abrir Mi Local para PDA"))
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir el PDF", Toast.LENGTH_LONG).show()
        }
    }
}
