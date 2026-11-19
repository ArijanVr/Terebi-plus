package ch.arjo.terebi.apps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class InstalledApp(val pkg: String, val label: String)

/** Startbare Apps über PackageManager. Keine Systemänderung: nur lesen und Launch-Intents starten. */
class AppRepository(private val context: Context) {
    private val pm: PackageManager get() = context.packageManager
    private val bannerCache = HashMap<String, ImageBitmap?>()
    private val iconCache = HashMap<String, ImageBitmap?>()

    /** TV-Apps (LEANBACK_LAUNCHER), dazu normale Launcher-Apps, ohne terebi+ selbst. */
    fun installed(): List<InstalledApp> {
        val tv = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)
        val phone = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        return (tv + phone)
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.pkg != context.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

    /** Original-Banner (16:9) der App, sonst ihr Icon. Einmal geladen, dann aus dem Speicher. */
    fun banner(pkg: String, widthPx: Int, heightPx: Int): ImageBitmap? = bannerCache.getOrPut("$pkg@$widthPx") {
        runCatching {
            val info = pm.getApplicationInfo(pkg, 0)
            val d: Drawable = info.loadBanner(pm)
                ?: pm.getLeanbackLaunchIntentForPackage(pkg)?.let { pm.getActivityBanner(it) }
                ?: return@runCatching info.loadIcon(pm).toBitmap(heightPx, heightPx).asImageBitmap()
            d.toBitmap(widthPx, heightPx).asImageBitmap()
        }.getOrNull()
    }

    fun icon(pkg: String, sizePx: Int): ImageBitmap? = iconCache.getOrPut("$pkg@$sizePx") {
        runCatching { pm.getApplicationIcon(pkg).toBitmap(sizePx, sizePx).asImageBitmap() }.getOrNull()
    }

    fun clearCache() { bannerCache.clear(); iconCache.clear() }

    /** Liefert false, wenn die App fehlt oder nicht startbar ist (wird dem Nutzer gemeldet). */
    fun launch(pkg: String): Boolean {
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg) ?: return false
        return start(intent)
    }

    fun openAppInfo(pkg: String) = start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null)))
    fun openSystemSettings() = start(Intent(Settings.ACTION_SETTINGS))
    fun openHomeSettings() = start(Intent(Settings.ACTION_HOME_SETTINGS)) || openSystemSettings()

    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
