package ch.arjo.terebi.ui

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

data class SceneSpec(val stops: List<Color>, val glow: Color, val glowX: Float, val glowY: Float, val glowAlpha: Float, val effect: String)
data class CodeInfo(val label: String, val icon: String)

/**
 * Design-Tokens aus assets/tokens.json (dieselbe Datei wie die Vorschau). Zur Laufzeit gelesen,
 * damit Vorschau und App nie auseinanderlaufen. Masse in px bei 1920 Breite; [unit] rechnet auf dp um.
 */
class Design(json: JSONObject, codesJson: JSONObject, screenWidthDp: Int, lowRam: Boolean = false) {
    private val unit = screenWidthDp / json.getJSONObject("layout").getDouble("canvasWidth").toFloat()
    fun px(v: Number): Dp = (v.toFloat() * unit).dp
    fun dp(v: Number): Dp = px(v.toFloat() * 2f)
    fun dpSp(v: Number): TextUnit = pxSp(v.toFloat() * 2f)
    private fun pxSp(v: Number): TextUnit = (v.toFloat() * unit).sp

    private val c = json.getJSONObject("color")
    val ink = color(c.getString("ink"))
    val text = color(c.getString("text"))
    val textSecondary = color(c.getString("textSecondary"))
    val accent = color(c.getString("accent"))
    val focusRing = color(c.getString("focusRing"))
    val scrim = color(c.getString("scrim"))
    // Glas ink.900 74 %; auf Low-RAM-Geräten deckend ink.800 92 % (Guide 7.6)
    val glass = if (lowRam) color("#16181E").copy(alpha = 0.92f) else color("#0E1014").copy(alpha = 0.74f)
    val cool = color("#8DB3E2")
    val warm = color("#F0C27B")

    private val t = json.getJSONObject("type")
    val display = pxSp(t.getInt("display"))
    val title = pxSp(t.getInt("title"))
    val body = pxSp(t.getInt("body"))
    val caption = pxSp(t.getInt("caption"))
    val tileLabel = pxSp(t.getInt("tileLabel"))

    private val l = json.getJSONObject("layout")
    val safeX = px(l.getInt("safeX"))
    val safeY = px(l.getInt("safeY"))
    val logoHeight = px(l.getInt("logoHeight"))
    val appW = px(l.getInt("appWidth"))
    val appH = px(l.getInt("appHeight"))
    val appGap = px(l.getInt("appGap"))
    val appColumns = l.getInt("appColumns")
    val appRadius = px(l.getInt("appRadius"))
    val sidebarWidth = px(l.getInt("sidebarWidth"))
    val sidebarTop = px(l.getInt("sidebarTop"))
    val sidebarHeight = px(l.getInt("sidebarHeight"))
    val sidebarTileWidth = px(l.getInt("sidebarTileWidth"))
    val sidebarTileHeight = px(l.getInt("sidebarTileHeight"))
    val sidebarGap = px(l.getInt("sidebarGap"))
    val sidebarQrSize = px(l.getInt("sidebarQrSize"))

    private val iconScales = json.getJSONObject("iconScale")
    fun iconScale(size: String) = iconScales.optDouble(size, 1.0).toFloat()

    private val f = json.getJSONObject("focus")
    val focusScale = f.getDouble("scale").toFloat()
    val ringWidth = px(f.getInt("ringWidth"))
    val ringOffset = px(f.getInt("ringOffset"))
    val dimUnfocused = f.getDouble("dimUnfocused").toFloat()

    private val m = json.getJSONObject("motion")
    val focusMs = m.getInt("focusMs")
    val overlayMs = m.getInt("overlayMs")
    val sceneMs = m.getInt("sceneCrossfadeMs")
    val longPressMs = m.getLong("longPressMs")
    val idleReduceAfterMs = m.getLong("idleReduceAfterMs")

    private val sceneTints = json.getJSONObject("sceneTint")
    fun sceneTint(id: String): Color = color(sceneTints.optString(id, sceneTints.getString("default")))

    val nightDim = json.getDouble("nightDim").toFloat()
    private val temperatureColors = json.getJSONArray("temperatureColors").let { a ->
        (0 until a.length()).map { color(a.getString(it)) }
    }

    fun temperatureColor(celsius: Double): Color {
        val position = ((celsius.coerceIn(-15.0, 35.0) + 15.0) / 50.0).toFloat()
        return lerp(temperatureColors.first(), temperatureColors.last(), position)
    }

    val scenes: Map<String, SceneSpec> = json.getJSONObject("scenes").let { s ->
        s.keys().asSequence().associateWith { k ->
            val o = s.getJSONObject(k)
            val st = o.getJSONArray("stops")
            SceneSpec(
                stops = (0 until st.length()).map { color(st.getString(it)) },
                glow = color(o.getString("glow")),
                glowX = o.getDouble("glowX").toFloat(),
                glowY = o.getDouble("glowY").toFloat(),
                glowAlpha = o.getDouble("glowAlpha").toFloat(),
                effect = o.getString("effect"),
            )
        }
    }

    private val codes = codesJson.getJSONObject("codes")
    fun code(code: Int): CodeInfo = (codes.optJSONObject(code.toString()) ?: codes.getJSONObject("3"))
        .let { CodeInfo(it.getString("label"), it.getString("icon")) }

    companion object {
        /** #RRGGBB oder #RRGGBBAA (CSS-Schreibweise). */
        fun color(hex: String): Color {
            val h = hex.removePrefix("#")
            val rgb = h.substring(0, 6).toLong(16)
            val a = if (h.length == 8) h.substring(6, 8).toInt(16) else 255
            return Color(((a.toLong() shl 24) or rgb).toInt())
        }

        fun load(context: Context, screenWidthDp: Int): Design {
            fun read(name: String) = JSONObject(context.assets.open(name).bufferedReader().use { it.readText() })
            val lowRam = (context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).isLowRamDevice
            return Design(read("tokens.json"), read("weather-codes.json"), screenWidthDp, lowRam)
        }
    }
}

val LocalDesign = staticCompositionLocalOf<Design> { error("Design fehlt") }
