package ch.arjo.terebi.apps

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * IP Television+ («TV+») liegt als signiertes APK im Launcher. terebi+ installiert nie still:
 * der System-Installer zeigt die übliche Bestätigung, und ohne Freigabe für unbekannte Apps
 * öffnet terebi+ nur die passende Android-Einstellung. Keine Systempakete, kein Root.
 */
object TvPlus {
    const val PACKAGE = "ch.arjo.iptelevision"
    const val VERSION = "1.2.0"

    /** Pfad im Asset-Ordner; die Datei wird mit dem Release von IP Television+ ersetzt. */
    const val ASSET = "bundled/iptelevision-$VERSION.apk"

    /** SHA-256 des signierten APK. Ändert sich die Datei, muss dieser Wert mitgezogen werden. */
    const val SHA256 = "2e8ac3f69cef3cc4a3342f168f970872ea4a4778fc78051a4cac4fb8be7dea96"

    enum class Result { OPENED, NEEDS_PERMISSION, BUNDLE_MISSING }

    fun isInstalled(context: Context): Boolean =
        runCatching { context.packageManager.getPackageInfo(PACKAGE, 0) }.isSuccess

    /** Das APK ist im Launcher vorhanden (Build-Prüfung, keine Installation). */
    fun hasBundle(context: Context): Boolean =
        runCatching { context.assets.open(ASSET).close() }.isSuccess

    /**
     * Kopiert das APK in den Cache, prüft die Prüfsumme und öffnet den System-Installer.
     * Die Kopie liegt unter `cache/install/` (siehe `res/xml/install_paths.xml`).
     */
    suspend fun install(context: Context): Result {
        if (!context.packageManager.canRequestPackageInstalls()) {
            openUnknownSources(context)
            return Result.NEEDS_PERMISSION
        }
        val apk = withContext(Dispatchers.IO) { stage(context) } ?: return Result.BUNDLE_MISSING
        val uri = FileProvider.getUriForFile(context, context.packageName + ".install", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }
            .fold({ Result.OPENED }, { openUnknownSources(context); Result.NEEDS_PERMISSION })
    }

    /** Kopie im Cache; null, wenn das Asset fehlt oder die Prüfsumme nicht stimmt. */
    fun stage(context: Context): File? = runCatching {
        val dir = File(context.cacheDir, "install").apply { mkdirs() }
        val out = File(dir, "IPTelevision-$VERSION.apk")
        context.assets.open(ASSET).use { input -> out.outputStream().use { input.copyTo(it) } }
        check(sha256(out).equals(SHA256, ignoreCase = true)) { "Prüfsumme stimmt nicht" }
        out
    }.getOrNull()

    private fun openUnknownSources(context: Context) {
        val packageUri = Uri.parse("package:" + context.packageName)
        val direct = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(direct) }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
