package ch.arjo.terebi.wifi

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * WLAN-Passwort, verschlüsselt mit AES-GCM; der Schlüssel liegt im Android Keystore
 * und verlässt das Gerät nie (Guide 13). Klartext wird nie gespeichert oder geloggt.
 */
class SecretStore(context: Context) {
    private val sp = context.getSharedPreferences("secure", Context.MODE_PRIVATE)

    fun savePassword(password: String) {
        if (password.isEmpty()) {
            clear()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val ct = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
        val blob = cipher.iv + ct
        sp.edit { putString(PREF, Base64.encodeToString(blob, Base64.NO_WRAP)) }
    }

    fun readPassword(): String? = runCatching {
        val blob = Base64.decode(sp.getString(PREF, null) ?: return null, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, blob, 0, IV_LEN))
        String(cipher.doFinal(blob, IV_LEN, blob.size - IV_LEN), Charsets.UTF_8)
    }.getOrNull()

    fun clear() = sp.edit { remove(PREF) }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "wifi_password_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREF = "wifi_password"
        const val IV_LEN = 12
    }
}
