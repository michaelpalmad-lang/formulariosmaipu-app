package cl.easymaipu.formularios

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Actualización dentro de la app.
 * - Consulta la última versión publicada en GitHub (Releases).
 * - Si es más nueva que la instalada, avisa y, al aceptar, descarga el APK dentro de la app.
 * - Luego abre el instalador de Android (la persona solo toca "Instalar").
 *   La primera vez Android pide permitir que esta app instale actualizaciones.
 */
class Updater(private val activity: AppCompatActivity) {

    companion object {
        private const val API = "https://api.github.com/repos/michaelpalmad-lang/formulariosmaipu-app/releases/latest"
        private const val MIN_INTERVAL_MS = 10 * 60 * 1000L  // revisa como máximo cada 10 minutos
    }

    private var lastCheck = 0L
    private var busy = false
    private var pendingApk: File? = null
    private var pendingVersion = 0L

    private fun installedCode(): Long {
        val pi = activity.packageManager.getPackageInfo(activity.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode
        else @Suppress("DEPRECATION") pi.versionCode.toLong()
    }

    /** Llamar en onResume: revisa sola cada cierto tiempo y retoma una instalación pendiente. */
    fun onResume() {
        val apk = pendingApk
        if (apk != null && canInstall()) {
            pendingApk = null
            install(apk)
            return
        }
        val now = System.currentTimeMillis()
        if (!busy && now - lastCheck > MIN_INTERVAL_MS) check(manual = false)
    }

    /** Revisión pedida por la persona (desde el panel oculto). */
    fun checkNow() = check(manual = true)

    private fun check(manual: Boolean) {
        if (busy) return
        busy = true
        lastCheck = System.currentTimeMillis()
        Thread {
            try {
                val conn = (URL(API).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "FormulariosMaipu-App")
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)
                val tag = json.optString("tag_name")                       // ej. "v1.14"
                val latest = tag.substringAfterLast('.').toLongOrNull() ?: 0L
                var apkUrl = ""
                var apkSize = 0L
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                            apkUrl = a.optString("browser_download_url")
                            apkSize = a.optLong("size")
                        }
                    }
                }
                val current = installedCode()
                activity.runOnUiThread {
                    busy = false
                    if (activity.isFinishing) return@runOnUiThread
                    if (latest > current && apkUrl.isNotEmpty()) {
                        askToUpdate(latest, current, apkUrl, apkSize)
                    } else if (manual) {
                        Toast.makeText(activity, "Ya tienes la última versión (1.$current)", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                activity.runOnUiThread {
                    busy = false
                    if (manual) Toast.makeText(activity, "No se pudo revisar la versión. Revise la conexión.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun askToUpdate(latest: Long, current: Long, url: String, size: Long) {
        val mb = if (size > 0) String.format(" (%.1f MB)", size / 1048576.0) else ""
        AlertDialog.Builder(activity)
            .setTitle("Nueva versión disponible")
            .setMessage("Está disponible la versión 1.$latest$mb. Tienes instalada la 1.$current.\n\nLa actualización se descarga e instala desde la app.")
            .setCancelable(false)
            .setPositiveButton("Actualizar") { _, _ -> download(url, latest) }
            .setNegativeButton("Más tarde", null)
            .show()
    }

    private fun download(url: String, latest: Long) {
        busy = true
        val pad = (20 * activity.resources.displayMetrics.density).toInt()
        val bar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            isIndeterminate = true
        }
        val label = TextView(activity).apply { text = "Descargando…"; setPadding(0, pad / 2, 0, 0) }
        val box = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(bar); addView(label)
        }
        var cancelled = false
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Actualizando")
            .setView(box)
            .setCancelable(false)
            .setNegativeButton("Cancelar") { _, _ -> cancelled = true }
            .show()

        Thread {
            val dir = File(activity.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "formularios-maipu.apk")
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "FormulariosMaipu-App")
                }
                val total = conn.contentLength.toLong()
                conn.inputStream.use { input ->
                    file.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        var lastPct = -1
                        while (true) {
                            if (cancelled) break
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (total > 0) {
                                val pct = (done * 100 / total).toInt()
                                if (pct != lastPct) {
                                    lastPct = pct
                                    activity.runOnUiThread {
                                        bar.isIndeterminate = false
                                        bar.progress = pct
                                        label.text = "Descargando… $pct%"
                                    }
                                }
                            }
                        }
                    }
                }
                conn.disconnect()
                activity.runOnUiThread {
                    busy = false
                    if (dialog.isShowing) dialog.dismiss()
                    if (cancelled) { file.delete(); return@runOnUiThread }
                    if (!isValidUpdate(file, latest)) {
                        file.delete()
                        Toast.makeText(activity, "La descarga no es válida. Intente nuevamente.", Toast.LENGTH_LONG).show()
                        return@runOnUiThread
                    }
                    if (canInstall()) install(file) else askPermission(file, latest)
                }
            } catch (e: Exception) {
                file.delete()
                activity.runOnUiThread {
                    busy = false
                    if (dialog.isShowing) dialog.dismiss()
                    if (!cancelled) Toast.makeText(activity, "No se pudo descargar la actualización. Revise la conexión.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    /** Comprueba que el archivo descargado sea esta misma app y una versión más nueva. */
    private fun isValidUpdate(file: File, latest: Long): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        val info = activity.packageManager.getPackageArchiveInfo(file.absolutePath, 0) ?: return false
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode
        else @Suppress("DEPRECATION") info.versionCode.toLong()
        return info.packageName == activity.packageName && code > installedCode() && code >= latest
    }

    private fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < 26 || activity.packageManager.canRequestPackageInstalls()

    private fun askPermission(file: File, latest: Long) {
        pendingApk = file
        pendingVersion = latest
        AlertDialog.Builder(activity)
            .setTitle("Permitir actualizaciones")
            .setMessage("Para instalar la actualización, Android necesita que permitas a esta app instalar actualizaciones. Solo se pide la primera vez.\n\nEn la pantalla que se abre, activa \"Permitir de esta fuente\" y vuelve a la app.")
            .setCancelable(false)
            .setPositiveButton("Ir a permitir") { _, _ ->
                try {
                    activity.startActivity(
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
                    )
                } catch (e: ActivityNotFoundException) {
                    activity.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                }
            }
            .setNegativeButton("Cancelar") { _, _ -> pendingApk = null }
            .show()
    }

    private fun install(file: File) {
        try {
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(activity, "No se pudo abrir el instalador.", Toast.LENGTH_LONG).show()
        }
    }
}
