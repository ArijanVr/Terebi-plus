package ch.arjo.terebi.wifi

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

enum class WifiSecurity(val code: String, val label: String) {
    WPA("WPA", "WPA/WPA2"),
    SAE("SAE", "WPA3"),
    WEP("WEP", "WEP"),
    NOPASS("nopass", "Offen"),
}

/** Quadratische QR-Matrix ohne Rand; true = dunkles Modul. */
class QrMatrix(val size: Int, private val bits: BooleanArray) {
    operator fun get(x: Int, y: Int): Boolean = bits[y * size + x]
}

object WifiQr {
    private val SPECIAL = setOf('\\', ';', ',', ':', '"')

    /** Maskiert `\ ; , : "` mit einem vorangestellten `\` . */
    fun escape(value: String): String = buildString(value.length + 8) {
        for (c in value) {
            if (c in SPECIAL) append('\\')
            append(c)
        }
    }

    /** `WIFI:T:<WPA|SAE|nopass>;S:<ssid>;P:<passwort>;H:true;;` – `P` fehlt bei offenem Netz, `H` nur bei verstecktem. */
    fun payload(ssid: String, password: String?, security: WifiSecurity, hidden: Boolean): String = buildString {
        append("WIFI:T:").append(security.code).append(';')
        append("S:").append(escape(ssid)).append(';')
        if (security != WifiSecurity.NOPASS) append("P:").append(escape(password.orEmpty())).append(';')
        if (hidden) append("H:true;")
        append(';')
    }

    /** ZXing core, Fehlerkorrektur M, ohne Rand (die Ruhezone zeichnet die Karte). */
    fun matrix(payload: String): QrMatrix {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 0,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val bm = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 0, 0, hints)
        val n = bm.width
        return QrMatrix(n, BooleanArray(n * n) { i -> bm.get(i % n, i / n) })
    }

    /**
     * Ganzzahlige Modulgrösse für eine Karte von [cardPx] mit mindestens [paddingPx] Innenabstand
     * und mindestens 4 Modulen Ruhezone.
     */
    fun modulePx(modules: Int, cardPx: Int, paddingPx: Int): Int {
        val byPadding = (cardPx - 2 * paddingPx) / modules
        val byQuietZone = cardPx / (modules + 8)
        return maxOf(1, minOf(byPadding, byQuietZone))
    }
}
