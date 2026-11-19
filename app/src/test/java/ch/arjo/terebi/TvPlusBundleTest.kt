package ch.arjo.terebi

import ch.arjo.terebi.apps.TvPlus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Das mitgelieferte TV+-APK muss unverändert im Asset-Ordner liegen (Version und Prüfsumme). */
class TvPlusBundleTest {
    @Test fun bundledApkIsThePublishedRelease() {
        assertEquals("ch.arjo.iptelevision", TvPlus.PACKAGE)
        assertTrue(TvPlus.ASSET.endsWith("iptelevision-${TvPlus.VERSION}.apk"))

        val apk = File("../assets/${TvPlus.ASSET}")
        assertTrue("TV+-APK fehlt: ${apk.path}", apk.isFile)

        val md = MessageDigest.getInstance("SHA-256")
        apk.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        val hash = md.digest().joinToString("") { "%02x".format(it) }
        assertEquals("TV+-APK weicht von der veröffentlichten Version ab", TvPlus.SHA256, hash)
    }
}
